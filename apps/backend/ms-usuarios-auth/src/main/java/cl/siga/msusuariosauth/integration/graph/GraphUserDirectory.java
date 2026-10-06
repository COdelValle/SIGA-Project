package cl.siga.msusuariosauth.integration.graph;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.JsonNode;

import cl.siga.coreshare.dto.usuario.CandidatoUsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cliente de Microsoft Graph (client credentials) usado para:
 * <ul>
 *   <li>Resolver el object id (oid) de Entra ID a partir de un correo.</li>
 *   <li>Aprovisionar cuentas del registro asíncrono: crear el usuario si no
 *       existe, fijar clave temporal con cambio obligatorio y asignar el app
 *       role. La operación es idempotente (reutiliza la cuenta existente).</li>
 *   <li>Sincronizar el rol local (tabla {@code usuarios}) con los app roles de la
 *       API, de modo que el claim {@code roles} del token refleje la BD.</li>
 * </ul>
 *
 * <p>Los errores se clasifican en {@link GraphTransientException} (reintentable)
 * y {@link GraphPermanentException} (no reintentable).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphUserDirectory {

    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";
    private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";

    private final GraphProperties properties;
    private final RestClient graphClient = RestClient.builder().baseUrl(GRAPH_BASE).build();

    private ClientSecretCredential credential;
    private volatile JsonNode apiServicePrincipal;

    public boolean isEnabled() {
        return properties.isConfigured();
    }

    public Optional<CandidatoUsuarioResponseDTO> findUserByEmail(String email) {
        requireEnabled();
        return buscarUsuario(email, "id,mail,userPrincipalName,displayName").map(user -> {
            String mail = user.path("mail").asText(null);
            String upn = user.path("userPrincipalName").asText(null);
            String displayName = user.path("displayName").asText(null);
            String resolvedEmail = (mail != null && !mail.isBlank()) ? mail : upn;
            return new CandidatoUsuarioResponseDTO(user.path("id").asText(), resolvedEmail, displayName);
        });
    }

    /**
     * Indica si el correo ya tiene una cuenta habilitada en Entra ID. Una cuenta
     * deshabilitada devuelve {@code false} para que el registro la reutilice
     * (reactivación). Sin Graph configurado devuelve vacío.
     */
    public Optional<Boolean> accountEnabledByEmail(String email) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return buscarUsuario(email, "id,accountEnabled")
                .map(user -> user.path("accountEnabled").asBoolean(true));
    }

    private Optional<JsonNode> buscarUsuario(String email, String select) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        // Escapa comillas simples para evitar inyeccion en el $filter de OData.
        String escaped = normalized.replace("'", "''");
        String filter = "mail eq '" + escaped + "' or userPrincipalName eq '" + escaped + "'";

        JsonNode body = get(ub -> ub.path("/users")
                .queryParam("$filter", filter)
                .queryParam("$select", select)
                .build());

        JsonNode value = body.path("value");
        if (!value.isArray() || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(value.get(0));
    }

    /**
     * Aprovisiona la cuenta del registro asíncrono de forma idempotente:
     * <ol>
     *   <li>Resuelve el oid por el hint (si viene) o por correo en Entra ID.</li>
     *   <li>Si no existe, crea la cuenta; si existe, le fija una clave nueva.</li>
     *   <li>En ambos casos deja la clave con cambio obligatorio y asigna el rol.</li>
     * </ol>
     * Nunca registra la contraseña en logs.
     */
    public ProvisionedUser provisionUser(String email, String displayName, Rol rol, String azureUserIdHint) {
        requireEnabled();
        String oid = (azureUserIdHint != null && !azureUserIdHint.isBlank())
                ? azureUserIdHint.trim()
                : findUserByEmail(email).map(CandidatoUsuarioResponseDTO::oid).orElse(null);

        String temporaryPassword = PasswordGenerator.generar();
        if (oid == null) {
            oid = createUser(email, displayName, temporaryPassword);
            log.info("Cuenta creada en Entra ID para {} ({})", email, oid);
        } else {
            // Cuenta previamente deshabilitada (soft delete) o existente: se reactiva.
            setAccountEnabled(oid, true);
            setTemporaryPassword(oid, temporaryPassword);
            log.info("Cuenta existente en Entra ID reutilizada y habilitada para {} ({})", email, oid);
        }
        syncRole(oid, rol);
        return new ProvisionedUser(oid, temporaryPassword);
    }

    /** Regenera la clave temporal (cambio obligatorio) de una cuenta existente. */
    public String resetPassword(String oid) {
        requireEnabled();
        String temporaryPassword = PasswordGenerator.generar();
        setTemporaryPassword(oid, temporaryPassword);
        return temporaryPassword;
    }

    /**
     * Deja al usuario con exactamente el app role correspondiente a {@code rol},
     * quitando cualquier otro app role de esta API.
     */
    public void syncRole(String oid, Rol rol) {
        requireEnabled();
        String resourceId = apiServicePrincipalId();
        String desiredRoleId = appRoleId(rol);
        Map<String, String> assigned = assignedRoles(oid, resourceId);

        if (!assigned.containsKey(desiredRoleId)) {
            assignAppRole(oid, resourceId, desiredRoleId);
        }
        assigned.forEach((roleId, assignmentId) -> {
            if (!roleId.equals(desiredRoleId)) {
                removeAppRoleAssignment(oid, assignmentId);
            }
        });
    }

    /** Habilita o deshabilita el inicio de sesión (soft delete / reactivación). */
    public void setAccountEnabled(String oid, boolean enabled) {
        requireEnabled();
        Map<String, Object> payload = Map.of("accountEnabled", enabled);
        try {
            graphClient.patch()
                    .uri(ub -> ub.path("/users/{oid}").build(oid))
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw graphError((enabled ? "habilitar" : "deshabilitar") + " la cuenta " + oid, ex);
        } catch (RestClientException ex) {
            throw new GraphTransientException(
                    "No se pudo contactar a Microsoft Graph para cambiar el estado de la cuenta.", ex);
        }
    }

    /** Invalida los refresh tokens vigentes del usuario (offboarding inmediato). */
    public void revokeSignInSessions(String oid) {
        requireEnabled();
        try {
            graphClient.post()
                    .uri(ub -> ub.path("/users/{oid}/revokeSignInSessions").build(oid))
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw graphError("revocar las sesiones del usuario " + oid, ex);
        } catch (RestClientException ex) {
            throw new GraphTransientException(
                    "No se pudo contactar a Microsoft Graph para revocar las sesiones.", ex);
        }
    }

    /** Quita todos los app roles de esta API al usuario (por ejemplo, al desactivarlo). */
    public void revokeRoles(String oid) {
        if (!isEnabled()) {
            log.warn("Graph no configurado: no se pudieron revocar los roles del usuario {}", oid);
            return;
        }
        String resourceId = apiServicePrincipalId();
        assignedRoles(oid, resourceId).values().forEach(assignmentId -> removeAppRoleAssignment(oid, assignmentId));
    }

    private String createUser(String email, String displayName, String temporaryPassword) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountEnabled", true);
        payload.put("displayName", displayName);
        payload.put("mailNickname", mailNickname(email));
        payload.put("userPrincipalName", email);
        payload.put("passwordProfile", Map.of(
                "password", temporaryPassword,
                "forceChangePasswordNextSignIn", true));

        try {
            JsonNode body = graphClient.post()
                    .uri(ub -> ub.path("/users").build())
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode id = body == null ? null : body.path("id");
            if (id == null || id.isMissingNode() || id.asText().isBlank()) {
                throw new GraphTransientException(
                        "Microsoft Graph no devolvió el identificador de la cuenta creada.");
            }
            return id.asText();
        } catch (RestClientResponseException ex) {
            throw graphError("crear la cuenta " + email, ex);
        } catch (RestClientException ex) {
            throw new GraphTransientException("No se pudo contactar a Microsoft Graph para crear la cuenta.", ex);
        }
    }

    private void setTemporaryPassword(String oid, String temporaryPassword) {
        Map<String, Object> payload = Map.of("passwordProfile", Map.of(
                "password", temporaryPassword,
                "forceChangePasswordNextSignIn", true));
        try {
            graphClient.patch()
                    .uri(ub -> ub.path("/users/{oid}").build(oid))
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw graphError("fijar la clave temporal del usuario " + oid, ex);
        } catch (RestClientException ex) {
            throw new GraphTransientException(
                    "No se pudo contactar a Microsoft Graph para fijar la clave temporal.", ex);
        }
    }

    private String mailNickname(String email) {
        String local = email == null ? "" : email.trim().toLowerCase();
        int arroba = local.indexOf('@');
        if (arroba > 0) {
            local = local.substring(0, arroba);
        }
        String limpio = local.replaceAll("[^a-z0-9._-]", "");
        if (limpio.isBlank()) {
            limpio = "usuario" + PasswordGenerator.generar().replaceAll("[^A-Za-z0-9]", "");
        }
        return limpio.length() > 64 ? limpio.substring(0, 64) : limpio;
    }

    private JsonNode apiServicePrincipalNode() {
        if (apiServicePrincipal == null) {
            String appId = properties.apiAppIdOrDefault();
            JsonNode body = get(ub -> ub.path("/servicePrincipals")
                    .queryParam("$filter", "appId eq '" + appId + "'")
                    .queryParam("$select", "id,appRoles")
                    .build());
            JsonNode value = body.path("value");
            if (!value.isArray() || value.isEmpty()) {
                throw new GraphPermanentException(
                        "No se encontró el service principal de la API en Entra ID (appId " + appId + ").");
            }
            apiServicePrincipal = value.get(0);
        }
        return apiServicePrincipal;
    }

    private String apiServicePrincipalId() {
        return apiServicePrincipalNode().path("id").asText();
    }

    private String appRoleId(Rol rol) {
        for (JsonNode appRole : apiServicePrincipalNode().path("appRoles")) {
            boolean enabled = appRole.path("isEnabled").asBoolean(true);
            if (enabled && rol.name().equals(appRole.path("value").asText(null))) {
                return appRole.path("id").asText();
            }
        }
        throw new GraphPermanentException(
                "El app role " + rol.name() + " no está definido en la app de API de Entra ID.");
    }

    private Map<String, String> assignedRoles(String oid, String resourceId) {
        JsonNode body = get(ub -> ub.path("/users/{oid}/appRoleAssignments").build(oid));
        Map<String, String> assignments = new HashMap<>();
        for (JsonNode assignment : body.path("value")) {
            if (resourceId.equals(assignment.path("resourceId").asText())) {
                assignments.put(assignment.path("appRoleId").asText(), assignment.path("id").asText());
            }
        }
        return assignments;
    }

    private void assignAppRole(String oid, String resourceId, String appRoleId) {
        Map<String, Object> payload = Map.of(
                "principalId", oid,
                "resourceId", resourceId,
                "appRoleId", appRoleId);
        // Una cuenta recien creada puede tardar en ser asignable (eventual
        // consistency de Entra ID): un 400/404 inmediato se reintenta antes de
        // considerarlo permanente.
        int intentosMax = 3;
        for (int intento = 1; intento <= intentosMax; intento++) {
            try {
                graphClient.post()
                        .uri(ub -> ub.path("/servicePrincipals/{id}/appRoleAssignedTo").build(resourceId))
                        .header(HttpHeaders.AUTHORIZATION, bearerHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                boolean replicacion = (status == 400 || status == 404) && intento < intentosMax;
                if (replicacion) {
                    log.warn("Asignacion de app role para {} devolvio HTTP {}; reintento {}/{} en {} s.",
                            oid, status, intento, intentosMax, intento * 2L);
                    dormir(intento * 2L);
                    continue;
                }
                throw graphError("asignar el app role " + appRoleId + " al usuario " + oid, ex);
            } catch (RestClientException ex) {
                throw new GraphTransientException(
                        "No se pudo contactar a Microsoft Graph para asignar el app role.", ex);
            }
        }
    }

    private void dormir(long segundos) {
        try {
            Thread.sleep(segundos * 1000L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GraphTransientException("Interrumpido esperando la replicacion de la cuenta en Entra ID.", ex);
        }
    }

    private void removeAppRoleAssignment(String oid, String assignmentId) {
        try {
            graphClient.delete()
                    .uri(ub -> ub.path("/users/{oid}/appRoleAssignments/{id}").build(oid, assignmentId))
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw graphError("quitar la asignación " + assignmentId + " del usuario " + oid, ex);
        } catch (RestClientException ex) {
            throw new GraphTransientException(
                    "No se pudo contactar a Microsoft Graph para quitar una asignación de rol.", ex);
        }
    }

    private JsonNode get(Function<UriBuilder, URI> uriFunction) {
        try {
            return graphClient.get()
                    .uri(uriFunction)
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw graphError("consultar Microsoft Graph", ex);
        } catch (RestClientException ex) {
            throw new GraphTransientException("No se pudo contactar a Microsoft Graph.", ex);
        }
    }

    private synchronized String bearerHeader() {
        try {
            if (credential == null) {
                credential = new ClientSecretCredentialBuilder()
                        .tenantId(properties.getTenantId())
                        .clientId(properties.getClientId())
                        .clientSecret(properties.getClientSecret())
                        .build();
            }
            AccessToken token = credential.getTokenSync(new TokenRequestContext().addScopes(GRAPH_SCOPE));
            return "Bearer " + token.getToken();
        } catch (RuntimeException ex) {
            throw new GraphTransientException("No se pudo obtener un token para Microsoft Graph.", ex);
        }
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new GraphPermanentException(
                    "La integración con Microsoft Graph no está configurada (falta AZURE_CLIENT_SECRET).");
        }
    }

    /**
     * Clasifica el error HTTP de Graph: 5xx, 408 y 429 son transitorios; el
     * resto de 4xx son permanentes. El mensaje no expone el cuerpo del error
     * (puede contener datos personales).
     */
    private RuntimeException graphError(String action, RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        log.error("Error de Microsoft Graph al {}: HTTP {}", action, status);
        if (status >= 500 || status == 408 || status == 429) {
            return new GraphTransientException(
                    "Microsoft Graph no está disponible al " + action + " (HTTP " + status + ").");
        }
        return new GraphPermanentException(
                "Microsoft Graph rechazó " + action + " (HTTP " + status + ").");
    }
}

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
import cl.siga.coreshare.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cliente de Microsoft Graph (client credentials) usado para:
 * <ul>
 *   <li>Resolver el object id (oid) de Entra ID a partir de un correo.</li>
 *   <li>Sincronizar el rol local (tabla {@code usuarios}) con los app roles de la API,
 *       de modo que el claim {@code roles} del token refleje la BD.</li>
 * </ul>
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
        String normalized = email == null ? "" : email.trim().toLowerCase();
        String filter = "mail eq '" + normalized + "' or userPrincipalName eq '" + normalized + "'";

        JsonNode body = get(ub -> ub.path("/users")
                .queryParam("$filter", filter)
                .queryParam("$select", "id,mail,userPrincipalName,displayName")
                .build());

        JsonNode value = body.path("value");
        if (!value.isArray() || value.isEmpty()) {
            return Optional.empty();
        }

        JsonNode user = value.get(0);
        String mail = user.path("mail").asText(null);
        String upn = user.path("userPrincipalName").asText(null);
        String displayName = user.path("displayName").asText(null);
        String resolvedEmail = (mail != null && !mail.isBlank()) ? mail : upn;

        return Optional.of(new CandidatoUsuarioResponseDTO(user.path("id").asText(), resolvedEmail, displayName));
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

    /** Quita todos los app roles de esta API al usuario (por ejemplo, al desactivarlo). */
    public void revokeRoles(String oid) {
        if (!isEnabled()) {
            log.warn("Graph no configurado: no se pudieron revocar los roles del usuario {}", oid);
            return;
        }
        String resourceId = apiServicePrincipalId();
        assignedRoles(oid, resourceId).values().forEach(assignmentId -> removeAppRoleAssignment(oid, assignmentId));
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
                throw new BusinessException(
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
        throw new BusinessException(
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
        try {
            graphClient.post()
                    .uri(ub -> ub.path("/servicePrincipals/{id}/appRoleAssignedTo").build(resourceId))
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw graphError("asignar el app role " + appRoleId + " al usuario " + oid, ex);
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
        }
    }

    private synchronized String bearerHeader() {
        if (credential == null) {
            credential = new ClientSecretCredentialBuilder()
                    .tenantId(properties.getTenantId())
                    .clientId(properties.getClientId())
                    .clientSecret(properties.getClientSecret())
                    .build();
        }
        AccessToken token = credential.getTokenSync(new TokenRequestContext().addScopes(GRAPH_SCOPE));
        return "Bearer " + token.getToken();
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new BusinessException(
                    "La integración con Microsoft Graph no está configurada (falta AZURE_CLIENT_SECRET).");
        }
    }

    private ServiceUnavailableException graphError(String action, RestClientResponseException ex) {
        log.error("Error de Microsoft Graph al {}: HTTP {}", action, ex.getStatusCode().value(), ex);
        return new ServiceUnavailableException(
                "Error de Microsoft Graph al " + action + " (HTTP " + ex.getStatusCode().value() + ").");
    }
}

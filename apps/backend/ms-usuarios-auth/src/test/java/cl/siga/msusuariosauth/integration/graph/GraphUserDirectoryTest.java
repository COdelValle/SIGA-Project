package cl.siga.msusuariosauth.integration.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;

import cl.siga.coreshare.dto.usuario.CandidatoUsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;

@ExtendWith(MockitoExtension.class)
class GraphUserDirectoryTest {

    private static final String BASE = "https://graph.microsoft.com/v1.0";

    @Mock
    private ClientSecretCredential credential;

    private GraphProperties properties;
    private GraphUserDirectory directory;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new GraphProperties();
        properties.setTenantId("tenant");
        properties.setClientId("client");
        properties.setClientSecret("secret");
        properties.setApiAppId("api-app");

        directory = new GraphUserDirectory(properties);
        lenient().when(credential.getTokenSync(any(TokenRequestContext.class)))
                .thenReturn(new AccessToken("token", OffsetDateTime.now().plusHours(1)));
        ReflectionTestUtils.setField(directory, "credential", credential);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(directory, "graphClient", builder.build());
    }

    private RequestMatcher ruta(HttpMethod metodo, String path) {
        return request -> {
            assertEquals(metodo, request.getMethod());
            assertEquals(path, request.getURI().getPath());
        };
    }

    private RequestMatcher rutaConQuery(HttpMethod metodo, String path, String fragmento) {
        return request -> {
            assertEquals(metodo, request.getMethod());
            assertEquals(path, request.getURI().getPath());
            assertTrue(request.getURI().getRawQuery().contains(fragmento));
        };
    }

    private String servicePrincipalJson() {
        return """
                {"value":[{"id":"sp-1","appRoles":[
                  {"id":"role-est","value":"ESTUDIANTE","isEnabled":true},
                  {"id":"role-doc","value":"DOCENTE","isEnabled":true},
                  {"id":"role-apo","value":"APODERADO","isEnabled":true},
                  {"id":"role-disabled","value":"ADMIN","isEnabled":false}
                ]}]}
                """;
    }

    @Test
    void isEnabledReflejaLaConfiguracion() {
        assertTrue(directory.isEnabled());
        properties.setClientSecret(null);
        assertFalse(directory.isEnabled());
    }

    @Test
    void findUserByEmailDevuelveElCandidato() {
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/users", "mail"))
                .andRespond(withSuccess("""
                        {"value":[{"id":"oid-1","mail":"ana@test.com",
                        "userPrincipalName":"ana@upn.com","displayName":"Ana"}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<CandidatoUsuarioResponseDTO> resultado = directory.findUserByEmail("ana@test.com");

        assertTrue(resultado.isPresent());
        assertEquals("oid-1", resultado.get().oid());
        assertEquals("ana@test.com", resultado.get().email());
        assertEquals("Ana", resultado.get().displayName());
        server.verify();
    }

    @Test
    void findUserByEmailUsaElUpnSiNoHayMail() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("""
                        {"value":[{"id":"oid-1","mail":"","userPrincipalName":"ana@upn.com",
                        "displayName":"Ana"}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<CandidatoUsuarioResponseDTO> resultado = directory.findUserByEmail("ana@upn.com");

        assertTrue(resultado.isPresent());
        assertEquals("ana@upn.com", resultado.get().email());
    }

    @Test
    void findUserByEmailDevuelveVacioSiNoHayResultados() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));

        assertTrue(directory.findUserByEmail("nadie@test.com").isEmpty());
    }

    @Test
    void accountEnabledDevuelveVacioSinGraphConfigurado() {
        properties.setClientSecret(null);

        assertTrue(directory.accountEnabledByEmail("ana@test.com").isEmpty());
    }

    @Test
    void accountEnabledLeeElEstadoDeLaCuenta() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("""
                        {"value":[{"id":"oid-1","accountEnabled":false}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(false), directory.accountEnabledByEmail("ana@test.com"));
    }

    @Test
    void accountEnabledAsumeTrueSiElCampoNoViene() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("{\"value\":[{\"id\":\"oid-1\"}]}", MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(true), directory.accountEnabledByEmail("ana@test.com"));
    }

    @Test
    void provisionUserReutilizaCuentaExistenteYSincronizaRol() {
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andExpect(jsonPath("$.accountEnabled").value(true))
                .andRespond(withSuccess());
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andExpect(jsonPath("$.passwordProfile.forceChangePasswordNextSignIn").value(true))
                .andRespond(withSuccess());
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/oid-1/appRoleAssignments"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/servicePrincipals/sp-1/appRoleAssignedTo"))
                .andExpect(jsonPath("$.appRoleId").value("role-est"))
                .andRespond(withSuccess());

        ProvisionedUser resultado = directory.provisionUser(
                "ana@test.com", "Ana", Rol.ESTUDIANTE, "oid-1");

        assertEquals("oid-1", resultado.oid());
        assertEquals(16, resultado.temporaryPassword().length());
        server.verify();
    }

    @Test
    void provisionUserCreaLaCuentaSiNoExiste() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/users"))
                .andExpect(jsonPath("$.userPrincipalName").value("nuevo@test.com"))
                .andExpect(jsonPath("$.mailNickname").value("nuevo"))
                .andExpect(jsonPath("$.passwordProfile.forceChangePasswordNextSignIn").value(true))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":\"new-oid\"}"));
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/new-oid/appRoleAssignments"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/servicePrincipals/sp-1/appRoleAssignedTo"))
                .andRespond(withSuccess());

        ProvisionedUser resultado = directory.provisionUser(
                "nuevo@test.com", "Nuevo", Rol.DOCENTE, null);

        assertEquals("new-oid", resultado.oid());
        server.verify();
    }

    @Test
    void provisionUserFallaSiGraphNoDevuelveElIdDeLaCuentaCreada() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/users"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

        assertThrows(GraphTransientException.class,
                () -> directory.provisionUser("nuevo@test.com", "Nuevo", Rol.DOCENTE, null));
    }

    @Test
    void provisionUserSinGraphConfiguradoLanzaErrorPermanente() {
        properties.setClientSecret(null);

        assertThrows(GraphPermanentException.class,
                () -> directory.provisionUser("ana@test.com", "Ana", Rol.ESTUDIANTE, null));
    }

    @Test
    void syncRoleQuitaLosRolesNoDeseados() {
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/oid-1/appRoleAssignments"))
                .andRespond(withSuccess("""
                        {"value":[
                          {"resourceId":"sp-1","appRoleId":"role-apo","id":"asignacion-1"},
                          {"resourceId":"otro-sp","appRoleId":"role-doc","id":"asignacion-externa"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/servicePrincipals/sp-1/appRoleAssignedTo"))
                .andRespond(withSuccess());
        server.expect(ruta(HttpMethod.DELETE, "/v1.0/users/oid-1/appRoleAssignments/asignacion-1"))
                .andRespond(withSuccess());

        directory.syncRole("oid-1", Rol.DOCENTE);

        server.verify();
    }

    @Test
    void syncRoleNoReasignaElRolQueYaTiene() {
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/oid-1/appRoleAssignments"))
                .andRespond(withSuccess("""
                        {"value":[{"resourceId":"sp-1","appRoleId":"role-doc","id":"asignacion-1"}]}
                        """, MediaType.APPLICATION_JSON));

        directory.syncRole("oid-1", Rol.DOCENTE);

        server.verify();
    }

    @Test
    void syncRoleFallaSiNoEncuentraElServicePrincipal() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/servicePrincipals"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(GraphPermanentException.class, () -> directory.syncRole("oid-1", Rol.DOCENTE));
    }

    @Test
    void syncRoleFallaSiElAppRoleNoEstaDefinido() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/servicePrincipals"))
                .andRespond(withSuccess("""
                        {"value":[{"id":"sp-1","appRoles":[
                          {"id":"role-est","value":"ESTUDIANTE","isEnabled":true}
                        ]}]}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(GraphPermanentException.class, () -> directory.syncRole("oid-1", Rol.DOCENTE));
    }

    @Test
    void resetPasswordRegeneraLaClaveTemporal() {
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andExpect(jsonPath("$.passwordProfile.forceChangePasswordNextSignIn").value(true))
                .andRespond(withSuccess());

        String password = directory.resetPassword("oid-1");

        assertEquals(16, password.length());
    }

    @Test
    void setAccountEnabledDeshabilitaLaCuenta() {
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andExpect(jsonPath("$.accountEnabled").value(false))
                .andRespond(withSuccess());

        directory.setAccountEnabled("oid-1", false);

        server.verify();
    }

    @Test
    void revokeSignInSessionsLlamaAlEndpointDeGraph() {
        server.expect(ruta(HttpMethod.POST, "/v1.0/users/oid-1/revokeSignInSessions"))
                .andRespond(withSuccess());

        directory.revokeSignInSessions("oid-1");

        server.verify();
    }

    @Test
    void revokeRolesQuitaTodasLasAsignacionesDeLaApi() {
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/oid-1/appRoleAssignments"))
                .andRespond(withSuccess("""
                        {"value":[
                          {"resourceId":"sp-1","appRoleId":"role-est","id":"asignacion-1"},
                          {"resourceId":"sp-1","appRoleId":"role-doc","id":"asignacion-2"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.DELETE, "/v1.0/users/oid-1/appRoleAssignments/asignacion-1"))
                .andRespond(withSuccess());
        server.expect(ruta(HttpMethod.DELETE, "/v1.0/users/oid-1/appRoleAssignments/asignacion-2"))
                .andRespond(withSuccess());

        directory.revokeRoles("oid-1");

        server.verify();
    }

    @Test
    void revokeRolesSinGraphConfiguradoNoHaceNada() {
        properties.setClientSecret(null);

        directory.revokeRoles("oid-1");

        server.verify();
    }

    @Test
    void findUserByEmailClasificaError500ComoTransitorio() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(GraphTransientException.class, () -> directory.findUserByEmail("ana@test.com"));
    }

    @Test
    void findUserByEmailClasificaError429ComoTransitorio() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(GraphTransientException.class, () -> directory.findUserByEmail("ana@test.com"));
    }

    @Test
    void findUserByEmailClasificaError400ComoPermanente() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(GraphPermanentException.class, () -> directory.findUserByEmail("ana@test.com"));
    }

    @Test
    void setAccountEnabledClasificaErroresDeGraph() {
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(GraphPermanentException.class, () -> directory.setAccountEnabled("oid-1", true));
    }

    @Test
    void revokeSignInSessionsClasificaErroresDeGraph() {
        server.expect(ruta(HttpMethod.POST, "/v1.0/users/oid-1/revokeSignInSessions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(GraphTransientException.class, () -> directory.revokeSignInSessions("oid-1"));
    }

    @Test
    void losMetodosFallanCuandoGraphNoEstaConfigurado() {
        properties.setClientSecret(null);

        assertThrows(GraphPermanentException.class, () -> directory.findUserByEmail("ana@test.com"));
        assertThrows(GraphPermanentException.class, () -> directory.resetPassword("oid-1"));
        assertThrows(GraphPermanentException.class, () -> directory.syncRole("oid-1", Rol.DOCENTE));
        assertThrows(GraphPermanentException.class, () -> directory.setAccountEnabled("oid-1", true));
        assertThrows(GraphPermanentException.class, () -> directory.revokeSignInSessions("oid-1"));
    }
}

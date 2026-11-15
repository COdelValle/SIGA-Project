package cl.siga.msusuariosauth.integration.graph;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;

import cl.siga.coreshare.dto.usuario.enums.Rol;

@ExtendWith(MockitoExtension.class)
class GraphUserDirectoryErroresTest {

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
            org.junit.jupiter.api.Assertions.assertEquals(metodo, request.getMethod());
            org.junit.jupiter.api.Assertions.assertEquals(path, request.getURI().getPath());
        };
    }

    private RequestMatcher rutaConQuery(HttpMethod metodo, String path, String fragmento) {
        return request -> {
            org.junit.jupiter.api.Assertions.assertEquals(metodo, request.getMethod());
            org.junit.jupiter.api.Assertions.assertEquals(path, request.getURI().getPath());
            org.junit.jupiter.api.Assertions.assertTrue(request.getURI().getRawQuery().contains(fragmento));
        };
    }

    private String servicePrincipalJson() {
        return """
                {"value":[{"id":"sp-1","appRoles":[
                  {"id":"role-est","value":"ESTUDIANTE","isEnabled":true},
                  {"id":"role-doc","value":"DOCENTE","isEnabled":true}
                ]}]}
                """;
    }

    private void responderErrorConexion(HttpMethod metodo, String path) {
        server.expect(ruta(metodo, path))
                .andRespond(request -> {
                    throw new ResourceAccessException("sin red");
                });
    }

    @Test
    void respuestaSinValueDevuelveVacio() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        org.assertj.core.api.Assertions.assertThat(directory.findUserByEmail("ana@test.com")).isEmpty();
    }

    @Test
    void erroresDeConexionSeClasificanComoTransitorios() {
        responderErrorConexion(HttpMethod.GET, "/v1.0/users");
        assertThatThrownBy(() -> directory.findUserByEmail("ana@test.com"))
                .isInstanceOf(GraphTransientException.class);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer servidorPatch = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(directory, "graphClient", builder.build());
        servidorPatch.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(request -> {
                    throw new ResourceAccessException("sin red");
                });
        assertThatThrownBy(() -> directory.setAccountEnabled("oid-1", true))
                .isInstanceOf(GraphTransientException.class);

        RestClient.Builder builderPost = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer servidorPost = MockRestServiceServer.bindTo(builderPost).build();
        ReflectionTestUtils.setField(directory, "graphClient", builderPost.build());
        servidorPost.expect(ruta(HttpMethod.POST, "/v1.0/users/oid-1/revokeSignInSessions"))
                .andRespond(request -> {
                    throw new ResourceAccessException("sin red");
                });
        assertThatThrownBy(() -> directory.revokeSignInSessions("oid-1"))
                .isInstanceOf(GraphTransientException.class);
    }

    @Test
    void errorAlFijarLaClaveTemporalSeClasificaSegunElStatus() {
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> directory.resetPassword("oid-1"))
                .isInstanceOf(GraphTransientException.class);
    }

    @Test
    void errorDeConexionAlFijarLaClaveTemporalEsTransitorio() {
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(request -> {
                    throw new ResourceAccessException("sin red");
                });

        assertThatThrownBy(() -> directory.resetPassword("oid-1"))
                .isInstanceOf(GraphTransientException.class)
                .hasMessageContaining("clave temporal");
    }

    @Test
    void laExcepcionPermanenteConservaMensajeYCausa() {
        IllegalStateException causa = new IllegalStateException("causa");

        GraphPermanentException error = new GraphPermanentException("fallo", causa);

        org.assertj.core.api.Assertions.assertThat(error.getMessage()).isEqualTo("fallo");
        org.assertj.core.api.Assertions.assertThat(error.getCause()).isSameAs(causa);
    }

    @Test
    void errorAlCrearLaCuentaSeClasificaSegunElStatus() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/users"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> directory.provisionUser(
                "nuevo@test.com", "Nuevo", Rol.DOCENTE, null))
                .isInstanceOf(GraphPermanentException.class);
    }

    @Test
    void error500AlCrearLaCuentaEsTransitorio() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/users"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> directory.provisionUser(
                "nuevo@test.com", "Nuevo", Rol.DOCENTE, null))
                .isInstanceOf(GraphTransientException.class);
    }

    @Test
    void errorDeConexionAlCrearLaCuentaEsTransitorio() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/users"))
                .andRespond(request -> {
                    throw new ResourceAccessException("sin red");
                });

        assertThatThrownBy(() -> directory.provisionUser(
                "nuevo@test.com", "Nuevo", Rol.DOCENTE, null))
                .isInstanceOf(GraphTransientException.class);
    }

    @Test
    void mailNicknameVacioUsaUnNombreGenerado() {
        server.expect(ruta(HttpMethod.GET, "/v1.0/users"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/users"))
                .andExpect(jsonPath("$.mailNickname").value(
                        org.hamcrest.Matchers.startsWith("usuario")))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":\"new-oid\"}"));
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/new-oid/appRoleAssignments"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/servicePrincipals/sp-1/appRoleAssignedTo"))
                .andRespond(withSuccess());

        directory.provisionUser("ΩΩΩ@test.com", "Nuevo", Rol.ESTUDIANTE, null);

        server.verify();
    }

    @Test
    void asignacionDeRolReintentaAnteUn404DeReplicacion() {
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(withSuccess());
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(withSuccess());
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/oid-1/appRoleAssignments"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/servicePrincipals/sp-1/appRoleAssignedTo"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(ruta(HttpMethod.POST, "/v1.0/servicePrincipals/sp-1/appRoleAssignedTo"))
                .andRespond(withSuccess());

        ProvisionedUser resultado = directory.provisionUser(
                "ana@test.com", "Ana", Rol.ESTUDIANTE, "oid-1");

        org.assertj.core.api.Assertions.assertThat(resultado.oid()).isEqualTo("oid-1");
        server.verify();
    }

    @Test
    void asignacionDeRolInterrumpidaSeReportaComoTransitoria() {
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(withSuccess());
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(withSuccess());
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/oid-1/appRoleAssignments"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/servicePrincipals/sp-1/appRoleAssignedTo"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> directory.provisionUser(
                    "ana@test.com", "Ana", Rol.ESTUDIANTE, "oid-1"))
                    .isInstanceOf(GraphTransientException.class)
                    .hasMessageContaining("Interrumpido");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void errorAlAsignarRolSinReplicacionEsPermanente() {
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(withSuccess());
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(withSuccess());
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/oid-1/appRoleAssignments"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/servicePrincipals/sp-1/appRoleAssignedTo"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> directory.provisionUser(
                "ana@test.com", "Ana", Rol.ESTUDIANTE, "oid-1"))
                .isInstanceOf(GraphTransientException.class);
    }

    @Test
    void errorDeConexionAlAsignarRolEsTransitorio() {
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(withSuccess());
        server.expect(ruta(HttpMethod.PATCH, "/v1.0/users/oid-1"))
                .andRespond(withSuccess());
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/oid-1/appRoleAssignments"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.POST, "/v1.0/servicePrincipals/sp-1/appRoleAssignedTo"))
                .andRespond(request -> {
                    throw new ResourceAccessException("sin red");
                });

        assertThatThrownBy(() -> directory.provisionUser(
                "ana@test.com", "Ana", Rol.ESTUDIANTE, "oid-1"))
                .isInstanceOf(GraphTransientException.class);
    }

    @Test
    void errorAlQuitarUnaAsignacionSeClasificaSegunElStatus() {
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/oid-1/appRoleAssignments"))
                .andRespond(withSuccess("""
                        {"value":[
                          {"resourceId":"sp-1","appRoleId":"role-doc","id":"asignacion-doc"},
                          {"resourceId":"sp-1","appRoleId":"role-est","id":"asignacion-est"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.DELETE, "/v1.0/users/oid-1/appRoleAssignments/asignacion-est"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> directory.syncRole("oid-1", Rol.DOCENTE))
                .isInstanceOf(GraphPermanentException.class);
    }

    @Test
    void errorDeConexionAlQuitarUnaAsignacionEsTransitorio() {
        server.expect(rutaConQuery(HttpMethod.GET, "/v1.0/servicePrincipals", "api-app"))
                .andRespond(withSuccess(servicePrincipalJson(), MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.GET, "/v1.0/users/oid-1/appRoleAssignments"))
                .andRespond(withSuccess("""
                        {"value":[
                          {"resourceId":"sp-1","appRoleId":"role-doc","id":"asignacion-doc"},
                          {"resourceId":"sp-1","appRoleId":"role-est","id":"asignacion-est"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(ruta(HttpMethod.DELETE, "/v1.0/users/oid-1/appRoleAssignments/asignacion-est"))
                .andRespond(request -> {
                    throw new ResourceAccessException("sin red");
                });

        assertThatThrownBy(() -> directory.syncRole("oid-1", Rol.DOCENTE))
                .isInstanceOf(GraphTransientException.class);
    }

    @Test
    void siNoSePuedeObtenerTokenSeReportaComoTransitorio() {
        ClientSecretCredential credencialRota = mock(ClientSecretCredential.class);
        when(credencialRota.getTokenSync(any(TokenRequestContext.class)))
                .thenThrow(new IllegalStateException("sin credenciales"));
        GraphUserDirectory directorioSinToken = new GraphUserDirectory(properties);
        ReflectionTestUtils.setField(directorioSinToken, "credential", credencialRota);

        assertThatThrownBy(() -> directorioSinToken.findUserByEmail("ana@test.com"))
                .isInstanceOf(GraphTransientException.class)
                .hasMessageContaining("token");
    }
}

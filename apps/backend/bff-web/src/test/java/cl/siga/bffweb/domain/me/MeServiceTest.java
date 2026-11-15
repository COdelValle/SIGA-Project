package cl.siga.bffweb.domain.me;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import cl.siga.bffweb.domain.me.dto.api.MeResponseDTO;
import cl.siga.bffweb.integration.usuarios.UsuarioClient;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;

class MeServiceTest {

    private final UsuarioClient usuarioClient = mock(UsuarioClient.class);
    private final MeService service = new MeService(usuarioClient);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usaElNombreDelRegistroCuandoExiste() {
        autenticar("oid-1", "Nombre del token");
        when(usuarioClient.getCurrentUsuario()).thenReturn(new UsuarioResponseDTO(
            "oid-1", "Camila Soto", "camila@siga.cl", Rol.ESTUDIANTE, StateUsuario.ACTIVO));

        MeResponseDTO me = service.getMe();

        assertThat(me.id()).isEqualTo("oid-1");
        assertThat(me.email()).isEqualTo("camila@siga.cl");
        assertThat(me.displayName()).isEqualTo("Camila Soto");
        assertThat(me.roles()).containsExactly(Rol.ESTUDIANTE);
        verify(usuarioClient).getCurrentUsuario();
    }

    @Test
    void usaElClaimNameDelTokenCuandoLaCuentaLegacyNoTraeNombre() {
        autenticar("oid-1", "Nombre del token");
        when(usuarioClient.getCurrentUsuario()).thenReturn(new UsuarioResponseDTO(
            "oid-1", null, "camila@siga.cl", Rol.ESTUDIANTE, StateUsuario.ACTIVO));

        assertThat(service.getMe().displayName()).isEqualTo("Nombre del token");
    }

    @Test
    void usaElCorreoCuandoElNombreDelRegistroYDelTokenEstanVacios() {
        autenticar("oid-1", "   ");
        when(usuarioClient.getCurrentUsuario()).thenReturn(new UsuarioResponseDTO(
            "oid-1", " ", "camila@siga.cl", Rol.ESTUDIANTE, StateUsuario.ACTIVO));

        assertThat(service.getMe().displayName()).isEqualTo("camila@siga.cl");
    }

    @Test
    void usaElCorreoCuandoNoHayToken() {
        when(usuarioClient.getCurrentUsuario()).thenReturn(new UsuarioResponseDTO(
            "oid-1", null, "camila@siga.cl", Rol.ESTUDIANTE, StateUsuario.ACTIVO));

        assertThat(service.getMe().displayName()).isEqualTo("camila@siga.cl");
    }

    @Test
    void traduce404DeMsUsuariosComoRecursoNoEncontrado() {
        when(usuarioClient.getCurrentUsuario()).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(service::getMe)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageStartingWith("El usuario autenticado no est");
    }

    @Test
    void traduceOtrosErroresFeignComoServicioNoDisponible() {
        FeignException causa = mock(FeignException.class);
        when(causa.status()).thenReturn(500);
        when(usuarioClient.getCurrentUsuario()).thenThrow(causa);

        assertThatThrownBy(service::getMe)
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessage("No se pudo obtener el usuario autenticado (HTTP 500).");
    }

    private static void autenticar(String oid, String nombre) {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("oid", oid)
            .claim("name", nombre)
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}

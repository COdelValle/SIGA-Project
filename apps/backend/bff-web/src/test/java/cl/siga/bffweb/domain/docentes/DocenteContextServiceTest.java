package cl.siga.bffweb.domain.docentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.ResourceNotFoundException;

class DocenteContextServiceTest {

    private final DocenteClient docenteClient = mock(DocenteClient.class);
    private final AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
    private final DocenteContextService service =
        new DocenteContextService(docenteClient, asignaturaClient);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validarCursoFallaCuandoLaDictacionNoExiste() {
        when(asignaturaClient.getCursoAsignaturaById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.validarCursoDelDocente(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Dictación con ID 99 no encontrada.");
    }

    @Test
    void adminPuedeValidarCualquierDictacionSinConsultarSuFicha() {
        autenticar("oid-admin", "ADMIN");
        CursoAsignaturaResponseDTO curso = curso();
        when(asignaturaClient.getCursoAsignaturaById(3L)).thenReturn(curso);

        var resultado = service.validarCursoDelDocente(3L);

        assertThat(resultado).isSameAs(curso);
        verifyNoInteractions(docenteClient);
    }

    @Test
    void cursosDelDocenteSinPaginaDevuelveListaVacia() {
        when(asignaturaClient.searchCursoAsignaturas(null, 1L, 100)).thenReturn(null);

        assertThat(service.cursosDelDocente(1L)).isEmpty();
    }

    @Test
    void cursosDelDocenteConPaginaNulaDevuelveListaVacia() {
        when(asignaturaClient.searchCursoAsignaturas(null, 1L, 100))
            .thenReturn(new cl.siga.coreshare.dto.common.PageResponseDTO<>(
                null, 0, 0, 100, 0, true, true, true));

        assertThat(service.cursosDelDocente(1L)).isEmpty();
    }

    private static void autenticar(String oid, String rol) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("oid", oid).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    private static CursoAsignaturaResponseDTO curso() {
        return new CursoAsignaturaResponseDTO(
            3L, 4L, "Ciencias Naturales", "ciencias naturales", AreaAcademica.CIENCIAS, true,
            CaracterAsignatura.OBLIGATORIA, Semestre.SEMESTRE_1, 1L, 4L, null, null, 0, List.of());
    }
}

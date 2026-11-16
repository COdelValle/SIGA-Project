package cl.siga.bffweb.domain.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import cl.siga.bffweb.domain.docentes.DocenteContextService;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;

class ReporteFiltroBffServiceTest {

    private final AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
    private final ClaseClient claseClient = mock(ClaseClient.class);
    private final EstudianteClient estudianteClient = mock(EstudianteClient.class);
    private final DocenteContextService docenteContext = mock(DocenteContextService.class);
    private final ReporteFiltroBffService service =
        new ReporteFiltroBffService(asignaturaClient, claseClient, estudianteClient, docenteContext);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminVeTodasLasDictacionesYLosEstudiantesDeLaClase() {
        autenticar("oid-admin", "ADMIN");
        when(asignaturaClient.searchCursoAsignaturas(isNull(), isNull(), anyInt()))
            .thenReturn(pagina(List.of(curso(8L, "Matemática", 4L))));
        when(claseClient.getClaseById(4L))
            .thenReturn(new ClaseResponseDTO(4L, Nivel.OCTAVO_BASICO, "A", 2026, null));
        when(asignaturaClient.getCursoAsignaturaById(8L))
            .thenReturn(curso(8L, "Matemática", 4L));
        when(estudianteClient.searchEstudiantesByClase(4L, 500))
            .thenReturn(pagina(List.of(new EstudianteResponseDTO(
                41L, "oid-41", "11111111-1", "Ana", null, "Pérez", null,
                LocalDate.of(2012, 5, 1), List.of(), "ACTIVO", 4L))));

        var filtros = service.filtros();
        var estudiantes = service.estudiantes(8L);

        assertThat(filtros.cursos()).hasSize(1);
        assertThat(filtros.cursos().get(0).nombre()).isEqualTo("Matemática");
        assertThat(filtros.cursos().get(0).curso()).contains("8vo Básico");
        assertThat(estudiantes).hasSize(1);
        assertThat(estudiantes.get(0).nombre()).isEqualTo("Ana Pérez");
        verify(asignaturaClient).searchCursoAsignaturas(null, null, 500);
    }

    @Test
    void docenteVeSoloSusDictacionesYValidaElCurso() {
        autenticar("oid-docente", "DOCENTE");
        when(docenteContext.docenteActual()).thenReturn(new DocenteResponseDTO(
            7L, "oid-docente", "Profesor", null, "Uno", null, "99999999-9",
            LocalDate.of(2020, 3, 1), true, null, List.of()));
        when(docenteContext.cursosDelDocente(7L))
            .thenReturn(List.of(curso(8L, "Matemática", 4L)));
        when(claseClient.getClaseById(4L))
            .thenReturn(new ClaseResponseDTO(4L, Nivel.OCTAVO_BASICO, "A", 2026, null));
        when(asignaturaClient.getCursoAsignaturaById(8L))
            .thenReturn(curso(8L, "Matemática", 4L));
        when(estudianteClient.searchEstudiantesByClase(eq(4L), anyInt()))
            .thenReturn(pagina(List.of()));

        var filtros = service.filtros();
        var estudiantes = service.estudiantes(8L);

        assertThat(filtros.cursos()).hasSize(1);
        assertThat(estudiantes).isEmpty();
        verify(docenteContext).validarCursoDelDocente(8L);
    }

    private static CursoAsignaturaResponseDTO curso(Long id, String nombre, Long idClase) {
        return new CursoAsignaturaResponseDTO(
            id, 1L, nombre, null, null, true, null, null, 7L, idClase,
            null, null, 0, List.of());
    }

    private static <T> PageResponseDTO<T> pagina(List<T> contenido) {
        return new PageResponseDTO<>(contenido, contenido.size(), 1, 500, 0,
            true, true, contenido.isEmpty());
    }

    private void autenticar(String oid, String rol) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("oid", oid).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
            List.of(new SimpleGrantedAuthority("ROLE_" + rol),
                new SimpleGrantedAuthority("SCOPE_asistencias:read"),
                new SimpleGrantedAuthority("SCOPE_notas:read"))));
    }
}

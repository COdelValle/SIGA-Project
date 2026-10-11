package cl.siga.bffweb.domain.docentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.enums.AreaAcademica;

class DocenteBffServiceTest {

    private final DocenteClient docenteClient = mock(DocenteClient.class);
    private final AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
    private final ClaseClient claseClient = mock(ClaseClient.class);
    private final EstudianteClient estudianteClient = mock(EstudianteClient.class);

    private final DocenteContextService docenteContext =
        new DocenteContextService(docenteClient, asignaturaClient);

    private final DocenteBffService service = new DocenteBffService(
        docenteContext, claseClient, estudianteClient);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCursosArmaCursoConAlumnosYDias() {
        autenticar();
        when(docenteClient.getDocenteByIdUsuario("oid-alejandro")).thenReturn(docente());
        when(asignaturaClient.searchCursoAsignaturasByDocente(eq(1L), anyInt()))
            .thenReturn(pagina(List.of(ciencias())));
        when(claseClient.getClaseById(4L)).thenReturn(new ClaseResponseDTO(4L, Nivel.OCTAVO_BASICO, "A", 2026, 1L));
        when(estudianteClient.searchEstudiantesByClase(eq(4L), anyInt())).thenReturn(pagina(List.of(estudiante())));

        var cursos = service.getCursos();

        assertThat(cursos).hasSize(1);
        assertThat(cursos.get(0).nombre()).isEqualTo("8vo Básico A");
        assertThat(cursos.get(0).nivel()).isEqualTo(8);
        assertThat(cursos.get(0).asignatura()).isEqualTo("Ciencias Naturales");
        assertThat(cursos.get(0).idAsignatura()).isEqualTo(4L);
        assertThat(cursos.get(0).diasClase()).containsExactly("Lunes", "Miércoles", "Viernes");
        assertThat(cursos.get(0).alumnos()).hasSize(1);
        assertThat(cursos.get(0).alumnos().get(0).nombres()).isEqualTo("CAMILA ANTONIETA");
    }

    @Test
    void getHorarioCalculaLaFranja() {
        autenticar();
        when(docenteClient.getDocenteByIdUsuario("oid-alejandro")).thenReturn(docente());
        when(asignaturaClient.searchCursoAsignaturasByDocente(eq(1L), anyInt()))
            .thenReturn(pagina(List.of(ciencias())));
        when(claseClient.getClaseById(4L)).thenReturn(new ClaseResponseDTO(4L, Nivel.OCTAVO_BASICO, "A", 2026, 1L));

        var horario = service.getHorario();

        assertThat(horario).isNotEmpty();
        assertThat(horario.get(0).franja()).isEqualTo(1);
        assertThat(horario.get(0).dia()).isEqualTo("Lunes");
        assertThat(horario.get(0).curso()).isEqualTo("8vo Básico A");
    }

    private static void autenticar() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("oid", "oid-alejandro").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_DOCENTE"))));
    }

    private static DocenteResponseDTO docente() {
        return new DocenteResponseDTO(1L, "oid-alejandro", "ALEJANDRO", "JAVIER", "SILVA", "MORALES",
            "11111111-1", LocalDate.of(2019, 3, 1), true, AreaAcademica.CIENCIAS, List.of());
    }

    private static CursoAsignaturaResponseDTO ciencias() {
        return new CursoAsignaturaResponseDTO(
            3L, 4L, "Ciencias Naturales", "ciencias naturales", AreaAcademica.CIENCIAS, true,
            CaracterAsignatura.OBLIGATORIA, Semestre.SEMESTRE_1, 1L, 4L, null, null, 0,
            List.of(
                new HorarioResponseDTO(1L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(8, 45), "Sala 8° Básico A"),
                new HorarioResponseDTO(2L, DiaSemana.MIERCOLES, LocalTime.of(9, 50), LocalTime.of(10, 35), "Sala 8° Básico A"),
                new HorarioResponseDTO(3L, DiaSemana.VIERNES, LocalTime.of(13, 55), LocalTime.of(14, 40), "Sala 8° Básico A")));
    }

    private static EstudianteResponseDTO estudiante() {
        return new EstudianteResponseDTO(
            1L, "oid-camila", "24112345-6", "CAMILA", "ANTONIETA", "SOTO", "HERNÁNDEZ",
            LocalDate.of(2012, 5, 15), List.of(), "REGISTRADO", 4L);
    }

    private static <T> PageResponseDTO<T> pagina(List<T> content) {
        return new PageResponseDTO<>(content, content.size(), 1, 200, 0, true, true, content.isEmpty());
    }
}

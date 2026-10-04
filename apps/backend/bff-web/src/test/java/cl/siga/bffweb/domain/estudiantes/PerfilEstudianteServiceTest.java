package cl.siga.bffweb.domain.estudiantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import cl.siga.bffweb.domain.estudiantes.dto.api.PerfilEstudianteResponseDTO;
import cl.siga.bffweb.domain.estudiantes.mapper.PerfilEstudianteMapperImpl;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.bffweb.integration.evaluaciones.EvaluacionClient;
import cl.siga.bffweb.integration.notas.NotaClient;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.enums.AreaAcademica;

class PerfilEstudianteServiceTest {

    private final EstudianteClient estudianteClient = mock(EstudianteClient.class);
    private final ClaseClient claseClient = mock(ClaseClient.class);
    private final AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
    private final EvaluacionClient evaluacionClient = mock(EvaluacionClient.class);
    private final NotaClient notaClient = mock(NotaClient.class);
    private final DocenteClient docenteClient = mock(DocenteClient.class);

    private final PerfilEstudianteService service = new PerfilEstudianteService(
        estudianteClient, claseClient, asignaturaClient, evaluacionClient, notaClient, docenteClient,
        new PerfilEstudianteMapperImpl());

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPerfilSigueElFlujoEstudianteClaseAsignaturaEvaluacionNota() {
        when(estudianteClient.getEstudianteById(1L)).thenReturn(estudiante(1L, "oid-camila", 4L));
        when(claseClient.getClaseById(4L)).thenReturn(new ClaseResponseDTO(4L, Nivel.OCTAVO_BASICO, "A", 2026, 1L));

        CursoAsignaturaResponseDTO matematica = dictacion(
            1L, 3L, "Matemática", AreaAcademica.MATEMATICAS, CaracterAsignatura.OBLIGATORIA,
            6L, 4L,
            List.of(new HorarioResponseDTO(10L, DiaSemana.MARTES, LocalTime.of(9, 50), LocalTime.of(10, 35), "Sala 8° Básico A")));
        when(asignaturaClient.searchCursoAsignaturasByClase(eq(4L), anyInt()))
            .thenReturn(pagina(List.of(matematica)));

        EvaluacionResponseDTO prueba = new EvaluacionResponseDTO(
            1L, "PRUEBA 1", TipoEvaluacion.SUMATIVA, 30.0, 1L, true);
        when(evaluacionClient.searchEvaluacionesByAsignatura(eq(1L), anyInt()))
            .thenReturn(pagina(List.of(prueba)));

        when(notaClient.searchNotas(eq(1L), anyInt()))
            .thenReturn(pagina(List.of(new NotaResponseDTO(100L, 1L, 1L, 6.5))));

        when(docenteClient.getDocenteById(6L)).thenReturn(new DocenteResponseDTO(
            6L, "oid-docente", "CAMILA", "ANTONIA", "CASTRO", "MEDINA", "23000003-4",
            LocalDate.of(2018, 4, 20), true, AreaAcademica.MATEMATICAS, List.of()));

        PerfilEstudianteResponseDTO perfil = service.getPerfil(1L);

        assertThat(perfil.id()).isEqualTo(1L);
        assertThat(perfil.idClase()).isEqualTo(4L);
        assertThat(perfil.clase().letra()).isEqualTo("A");
        assertThat(perfil.clase().nivel()).isEqualTo("8vo Básico");
        assertThat(perfil.asignaturas()).hasSize(1);
        assertThat(perfil.asignaturas().get(0).idAsignatura()).isEqualTo(3L);
        assertThat(perfil.asignaturas().get(0).calificable()).isTrue();
        assertThat(perfil.asignaturas().get(0).horarios()).hasSize(1);
        assertThat(perfil.asignaturas().get(0).horarios().get(0).dia()).isEqualTo("Martes");
        assertThat(perfil.asignaturas().get(0).docente()).isEqualTo("CAMILA ANTONIA CASTRO MEDINA");
        assertThat(perfil.asignaturas().get(0).evaluaciones()).hasSize(1);
        assertThat(perfil.asignaturas().get(0).evaluaciones().get(0).nota()).isEqualTo(6.5);
    }

    @Test
    void getPerfilIncluyeLasElectivasInscritasPorElEstudiante() {
        when(estudianteClient.getEstudianteById(1L)).thenReturn(estudiante(1L, "oid-camila", 4L));
        when(claseClient.getClaseById(4L)).thenReturn(new ClaseResponseDTO(4L, Nivel.OCTAVO_BASICO, "A", 2026, 1L));

        CursoAsignaturaResponseDTO matematica = dictacion(
            1L, 3L, "Matemática", AreaAcademica.MATEMATICAS, CaracterAsignatura.OBLIGATORIA,
            6L, 4L,
            List.of(new HorarioResponseDTO(10L, DiaSemana.MARTES, LocalTime.of(9, 50), LocalTime.of(10, 35), "Sala 8° Básico A")));
        when(asignaturaClient.searchCursoAsignaturasByClase(eq(4L), anyInt()))
            .thenReturn(pagina(List.of(matematica)));

        // Camila inscrita en la optativa Educación Financiera de su curso.
        when(asignaturaClient.searchInscripcionesByAlumno(eq(1L), anyInt())).thenReturn(pagina(List.of(
            new InscripcionResponseDTO(1L, 1L, 6L, EstadoInscripcion.ACTIVO, LocalDateTime.now()))));
        CursoAsignaturaResponseDTO educacionFisica = dictacion(
            6L, 13L, "Educación Física y Salud", AreaAcademica.EDUCACION_FISICA, CaracterAsignatura.OBLIGATORIA,
            3L, 4L,
            List.of(new HorarioResponseDTO(20L, DiaSemana.LUNES, LocalTime.of(13, 55), LocalTime.of(14, 40), "Cancha Techada 1")));
        when(asignaturaClient.getCursoAsignaturaById(6L)).thenReturn(educacionFisica);

        when(evaluacionClient.searchEvaluacionesByAsignatura(anyLong(), anyInt()))
            .thenReturn(pagina(List.of()));
        when(notaClient.searchNotas(eq(1L), anyInt())).thenReturn(pagina(List.of()));
        when(docenteClient.getDocenteById(anyLong())).thenReturn(null);

        PerfilEstudianteResponseDTO perfil = service.getPerfil(1L);

        assertThat(perfil.asignaturas()).hasSize(2);
        assertThat(perfil.asignaturas().stream().map(asignatura -> asignatura.name()))
            .containsExactly("Matemática", "Educación Física y Salud");
        assertThat(perfil.asignaturas().get(1).horarios().get(0).dia()).isEqualTo("Lunes");
    }

    @Test
    void getPerfilSinClaseDevuelveAsignaturasVacias() {
        when(estudianteClient.getEstudianteById(9L)).thenReturn(estudiante(9L, "oid-x", null));

        PerfilEstudianteResponseDTO perfil = service.getPerfil(9L);

        assertThat(perfil.clase()).isNull();
        assertThat(perfil.asignaturas()).isEmpty();
    }

    @Test
    void getPerfilMeResuelveElOidDelToken() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("oid", "oid-camila")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        when(estudianteClient.getEstudianteByIdUsuario("oid-camila")).thenReturn(estudiante(1L, "oid-camila", null));

        PerfilEstudianteResponseDTO perfil = service.getPerfilMe();

        assertThat(perfil.id()).isEqualTo(1L);
        verify(estudianteClient).getEstudianteByIdUsuario("oid-camila");
    }

    private static CursoAsignaturaResponseDTO dictacion(
            Long id, Long idAsignatura, String nombre, AreaAcademica area, CaracterAsignatura caracter,
            Long idDocente, Long idClase, List<HorarioResponseDTO> horarios) {
        return new CursoAsignaturaResponseDTO(
            id, idAsignatura, nombre, nombre.toLowerCase(), area, true, caracter,
            Semestre.SEMESTRE_1, idDocente, idClase, null, null, 0, horarios);
    }

    private static EstudianteResponseDTO estudiante(Long id, String oid, Long idClase) {
        return new EstudianteResponseDTO(
            id, oid, "24112345-6", "CAMILA", "ANTONIETA", "SOTO", "HERNÁNDEZ",
            LocalDate.of(2012, 5, 15), List.of(), "REGISTRADO", idClase);
    }

    private static <T> PageResponseDTO<T> pagina(List<T> content) {
        return new PageResponseDTO<>(content, content.size(), 1, 200, 0, true, true, content.isEmpty());
    }
}

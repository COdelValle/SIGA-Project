package cl.siga.bffweb.domain.notas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import cl.siga.bffweb.domain.docentes.DocenteContextService;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.bffweb.integration.evaluaciones.EvaluacionClient;
import cl.siga.bffweb.integration.notas.NotaClient;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;

class NotaBffServiceTest {

    private final NotaClient notaClient = mock(NotaClient.class);
    private final EvaluacionClient evaluacionClient = mock(EvaluacionClient.class);
    private final ClaseClient claseClient = mock(ClaseClient.class);
    private final EstudianteClient estudianteClient = mock(EstudianteClient.class);
    private final DocenteClient docenteClient = mock(DocenteClient.class);
    private final AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
    private final DocenteContextService docenteContext =
        new DocenteContextService(docenteClient, asignaturaClient);

    private final NotaBffService service = new NotaBffService(
        notaClient, evaluacionClient, claseClient, estudianteClient, docenteContext);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCursoNotasArmaEvaluacionesYNotas() {
        autenticar("oid-alejandro");
        cuandoEsDuenio();
        when(claseClient.getClaseById(4L))
            .thenReturn(new ClaseResponseDTO(4L, Nivel.OCTAVO_BASICO, "A", 2026, 1L));
        when(evaluacionClient.searchEvaluacionesByAsignatura(eq(3L), anyInt()))
            .thenReturn(pagina(List.of(evaluacion(9L, "PRUEBA", 60.0))));
        when(notaClient.searchNotasByEvaluacion(eq(9L), anyInt()))
            .thenReturn(pagina(List.of(new NotaResponseDTO(7L, 1L, 9L, 6.5))));
        when(estudianteClient.searchEstudiantesByClase(eq(4L), anyInt()))
            .thenReturn(pagina(List.of(estudiante())));

        var curso = service.getCursoNotas(3L);

        assertThat(curso.curso()).isEqualTo("8vo Básico A");
        assertThat(curso.asignatura()).isEqualTo("Ciencias Naturales");
        assertThat(curso.evaluaciones()).hasSize(1);
        assertThat(curso.evaluaciones().get(0).tipo()).isEqualTo("SUMATIVA");
        assertThat(curso.alumnos()).hasSize(1);
        assertThat(curso.alumnos().get(0).notas()).hasSize(1);
        assertThat(curso.alumnos().get(0).notas().get(0).score()).isEqualTo(6.5);
    }

    @Test
    void crearNotaValidaPertenenciaDeLaEvaluacion() {
        autenticar("oid-alejandro");
        cuandoEsDuenio();
        when(evaluacionClient.getEvaluacionById(9L)).thenReturn(evaluacion(9L, "PRUEBA", 60.0));
        RegistrarNotaRequestDTO request = new RegistrarNotaRequestDTO(1L, 9L, 6.0);
        when(notaClient.saveNota(request)).thenReturn(new NotaResponseDTO(20L, 1L, 9L, 6.0));

        var creada = service.crearNota(request);

        assertThat(creada.id()).isEqualTo(20L);
    }

    @Test
    void crearNotaRechazaAsignaturaDeOtroDocente() {
        autenticar("oid-otro");
        when(asignaturaClient.getCursoAsignaturaById(3L)).thenReturn(ciencias());
        when(docenteClient.getDocenteByIdUsuario("oid-otro")).thenReturn(docente(2L, "oid-otro"));
        when(evaluacionClient.getEvaluacionById(9L)).thenReturn(evaluacion(9L, "PRUEBA", 60.0));

        assertThatThrownBy(() -> service.crearNota(new RegistrarNotaRequestDTO(1L, 9L, 6.0)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void editarNotaValidaPorLaNotaExistente() {
        autenticar("oid-alejandro");
        cuandoEsDuenio();
        when(notaClient.getNotaById(7L)).thenReturn(new NotaResponseDTO(7L, 1L, 9L, 5.0));
        when(evaluacionClient.getEvaluacionById(9L)).thenReturn(evaluacion(9L, "PRUEBA", 60.0));
        when(notaClient.updateNota(eq(7L), eq(new ActualizarNotaRequestDTO(6.8))))
            .thenReturn(new NotaResponseDTO(7L, 1L, 9L, 6.8));

        var editada = service.editarNota(7L, new ActualizarNotaRequestDTO(6.8));

        assertThat(editada.score()).isEqualTo(6.8);
    }

    @Test
    void eliminarNotaValidaYDelega() {
        autenticar("oid-alejandro");
        cuandoEsDuenio();
        when(notaClient.getNotaById(7L)).thenReturn(new NotaResponseDTO(7L, 1L, 9L, 5.0));
        when(evaluacionClient.getEvaluacionById(9L)).thenReturn(evaluacion(9L, "PRUEBA", 60.0));

        service.eliminarNota(7L);

        verify(notaClient).deleteNota(7L);
    }

    private void cuandoEsDuenio() {
        when(asignaturaClient.getCursoAsignaturaById(3L)).thenReturn(ciencias());
        when(docenteClient.getDocenteByIdUsuario("oid-alejandro")).thenReturn(docente(1L, "oid-alejandro"));
    }

    private static void autenticar(String oid) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("oid", oid).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_DOCENTE"))));
    }

    private static DocenteResponseDTO docente(Long id, String oid) {
        return new DocenteResponseDTO(id, oid, "ALEJANDRO", "JAVIER", "SILVA", "MORALES",
            "11111111-1", LocalDate.of(2019, 3, 1), true, AreaAcademica.CIENCIAS, List.of());
    }

    private static CursoAsignaturaResponseDTO ciencias() {
        return new CursoAsignaturaResponseDTO(
            3L, 4L, "Ciencias Naturales", "ciencias naturales", AreaAcademica.CIENCIAS, true,
            CaracterAsignatura.OBLIGATORIA, Semestre.SEMESTRE_1, 1L, 4L, null, null, 0, List.of());
    }

    private static EvaluacionResponseDTO evaluacion(Long id, String nombre, Double ponderacion) {
        return new EvaluacionResponseDTO(id, nombre, TipoEvaluacion.SUMATIVA, ponderacion, 3L, true);
    }

    private static EstudianteResponseDTO estudiante() {
        return new EstudianteResponseDTO(
            1L, "oid-camila", "24112345-6", "CAMILA", "ANTONIETA", "SOTO", "HERNÁNDEZ",
            LocalDate.of(2012, 5, 15), List.of(), "REGISTRADO", 4L);
    }

    private static <T> PageResponseDTO<T> pagina(List<T> content) {
        return new PageResponseDTO<>(content, content.size(), 1, 100, 0, true, true, content.isEmpty());
    }
}

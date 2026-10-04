package cl.siga.bffweb.domain.notas;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import cl.siga.bffweb.domain.docentes.DocenteContextService;
import cl.siga.bffweb.domain.notas.dto.AlumnoNotasDTO;
import cl.siga.bffweb.domain.notas.dto.CursoNotasDTO;
import cl.siga.bffweb.domain.notas.dto.EvaluacionNotasDTO;
import cl.siga.bffweb.domain.notas.dto.NotaCursoDTO;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.bffweb.integration.evaluaciones.EvaluacionClient;
import cl.siga.bffweb.integration.notas.NotaClient;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Notas y evaluaciones del curso para el portal docente. Valida que la
 * asignatura pertenezca al docente autenticado antes de leer o escribir.
 */
@Service
@RequiredArgsConstructor
public class NotaBffService {
    private static final int PAGE_SIZE = 100;

    private final NotaClient notaClient;
    private final EvaluacionClient evaluacionClient;
    private final ClaseClient claseClient;
    private final EstudianteClient estudianteClient;
    private final DocenteContextService docenteContext;

    public CursoNotasDTO getCursoNotas(Long asignaturaId) {
        AsignaturaResponseDTO asignatura = docenteContext.validarAsignaturaDelDocente(asignaturaId);
        ClaseResponseDTO clase = asignatura.idClase() == null
            ? null
            : claseClient.getClaseById(asignatura.idClase());

        List<EvaluacionResponseDTO> evaluaciones = contentOf(
            evaluacionClient.searchEvaluacionesByAsignatura(asignaturaId, PAGE_SIZE));

        Map<Long, Map<Long, NotaResponseDTO>> notasPorEvaluacion = new HashMap<>();
        for (EvaluacionResponseDTO evaluacion : evaluaciones) {
            Map<Long, NotaResponseDTO> porEstudiante = contentOf(
                    notaClient.searchNotasByEvaluacion(evaluacion.id(), PAGE_SIZE)).stream()
                .filter(nota -> nota.idEstudiante() != null)
                .collect(Collectors.toMap(
                    NotaResponseDTO::idEstudiante, Function.identity(), (primera, repetida) -> primera));
            notasPorEvaluacion.put(evaluacion.id(), porEstudiante);
        }

        List<AlumnoNotasDTO> alumnos = asignatura.idClase() == null
            ? List.of()
            : contentOf(estudianteClient.searchEstudiantesByClase(asignatura.idClase(), PAGE_SIZE)).stream()
                .map(estudiante -> new AlumnoNotasDTO(
                    estudiante.id(),
                    nombres(estudiante),
                    estudiante.firstSurname(),
                    estudiante.secondSurname(),
                    notasDeAlumno(estudiante.id(), evaluaciones, notasPorEvaluacion)))
                .toList();

        return new CursoNotasDTO(
            asignatura.id(),
            clase == null ? asignatura.name() : nombreClase(clase),
            asignatura.name(),
            evaluaciones.stream()
                .map(evaluacion -> new EvaluacionNotasDTO(
                    evaluacion.id(),
                    evaluacion.nombre(),
                    evaluacion.tipo() == null ? null : evaluacion.tipo().name(),
                    evaluacion.ponderacion()))
                .toList(),
            alumnos);
    }

    public NotaResponseDTO crearNota(RegistrarNotaRequestDTO request) {
        validarEvaluacionDelDocente(request.idEvaluacion());
        return notaClient.saveNota(request);
    }

    public NotaResponseDTO editarNota(Long id, ActualizarNotaRequestDTO request) {
        NotaResponseDTO nota = notaDe(id);
        validarEvaluacionDelDocente(nota.idEvaluacion());
        return notaClient.updateNota(id, request);
    }

    public void eliminarNota(Long id) {
        NotaResponseDTO nota = notaDe(id);
        validarEvaluacionDelDocente(nota.idEvaluacion());
        notaClient.deleteNota(id);
    }

    private NotaResponseDTO notaDe(Long id) {
        NotaResponseDTO nota = notaClient.getNotaById(id);
        if (nota == null) {
            throw new ResourceNotFoundException("Nota con ID " + id + " no encontrada.");
        }
        return nota;
    }

    private void validarEvaluacionDelDocente(Long idEvaluacion) {
        EvaluacionResponseDTO evaluacion = evaluacionDe(idEvaluacion);
        docenteContext.validarAsignaturaDelDocente(evaluacion.idAsignatura());
    }

    private EvaluacionResponseDTO evaluacionDe(Long idEvaluacion) {
        EvaluacionResponseDTO evaluacion = evaluacionClient.getEvaluacionById(idEvaluacion);
        if (evaluacion == null) {
            throw new ResourceNotFoundException(
                "Evaluación con ID " + idEvaluacion + " no encontrada.");
        }
        return evaluacion;
    }

    private static List<NotaCursoDTO> notasDeAlumno(
            Long idEstudiante,
            List<EvaluacionResponseDTO> evaluaciones,
            Map<Long, Map<Long, NotaResponseDTO>> notasPorEvaluacion) {
        return evaluaciones.stream()
            .map(evaluacion -> notasPorEvaluacion
                .getOrDefault(evaluacion.id(), Collections.emptyMap())
                .get(idEstudiante))
            .filter(nota -> nota != null)
            .map(nota -> new NotaCursoDTO(nota.id(), nota.idEvaluacion(), nota.score()))
            .toList();
    }

    private static String nombres(EstudianteResponseDTO estudiante) {
        return Stream.of(estudiante.firstName(), estudiante.middleName())
            .filter(parte -> parte != null && !parte.isBlank())
            .collect(Collectors.joining(" "));
    }

    private static String nombreClase(ClaseResponseDTO clase) {
        return clase.nivel().getDescripcion() + " " + clase.letra();
    }

    private static <T> List<T> contentOf(PageResponseDTO<T> pagina) {
        return pagina == null || pagina.content() == null ? List.of() : pagina.content();
    }
}

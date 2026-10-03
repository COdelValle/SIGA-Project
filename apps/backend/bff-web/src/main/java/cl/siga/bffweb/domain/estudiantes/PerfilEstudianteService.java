package cl.siga.bffweb.domain.estudiantes;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.bffweb.domain.estudiantes.dto.api.PerfilEstudianteResponseDTO;
import cl.siga.bffweb.domain.estudiantes.dto.internal.AsignaturaDetalleDTO;
import cl.siga.bffweb.domain.estudiantes.dto.internal.EvaluacionDetalleDTO;
import cl.siga.bffweb.domain.estudiantes.dto.internal.HorarioDetalleDTO;
import cl.siga.bffweb.domain.estudiantes.mapper.PerfilEstudianteMapper;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.bffweb.integration.evaluaciones.EvaluacionClient;
import cl.siga.bffweb.integration.notas.NotaClient;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.security.SecurityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Orquesta el perfil academico siguiendo el flujo
 * estudiante -> clase -> asignaturas -> evaluaciones -> notas.
 */
@Service 
@RequiredArgsConstructor 
public class PerfilEstudianteService {
    private static final int PAGE_SIZE = 200;

    private final EstudianteClient estudianteClient;
    private final ClaseClient claseClient;
    private final AsignaturaClient asignaturaClient;
    private final EvaluacionClient evaluacionClient;
    private final NotaClient notaClient;
    private final DocenteClient docenteClient;

    private final PerfilEstudianteMapper mapper;

    @Transactional (readOnly = true)
    public PerfilEstudianteResponseDTO getPerfil(Long id) {
        return buildPerfil(estudianteClient.getEstudianteById(id));
    }

    @Transactional (readOnly = true)
    public PerfilEstudianteResponseDTO getPerfilMe() {
        String oid = SecurityUtils.getCurrentUserOid()
            .orElseThrow(() -> new AccessDeniedException("No se pudo identificar al usuario autenticado."));
        return buildPerfil(estudianteClient.getEstudianteByIdUsuario(oid));
    }

    private PerfilEstudianteResponseDTO buildPerfil(EstudianteResponseDTO estudiante) {
        // 1. Estudiante -> clase
        ClaseResponseDTO clase = estudiante.idClase() == null
            ? null
            : claseClient.getClaseById(estudiante.idClase());

        // 2. Clase -> asignaturas basicas + electivas inscritas por el estudiante
        List<AsignaturaResponseDTO> basicas = estudiante.idClase() == null
            ? Collections.emptyList()
            : contentOf(asignaturaClient.searchAsignaturasByClase(estudiante.idClase(), PAGE_SIZE));
        List<AsignaturaResponseDTO> asignaturas =
            unirAsignaturas(basicas, electivasDelEstudiante(estudiante.id()));

        // 3. Notas del estudiante (se cruzan por evaluacion); solo si hay asignaturas
        Map<Long, Double> notaPorEvaluacion = asignaturas.isEmpty()
            ? Collections.emptyMap()
            : notasPorEvaluacion(estudiante.id());

        // 4. Nombres de los docentes de cada asignatura (cache por request)
        Map<Long, String> docentePorId = docentePorId(asignaturas);

        // 5. Asignaturas -> evaluaciones -> nota
        List<AsignaturaDetalleDTO> detalle = asignaturas.stream()
            .map(asignatura -> toDetalle(asignatura, notaPorEvaluacion, docentePorId))
            .toList();

        return mapper.toResponse(estudiante, clase, detalle);
    }

    /** Electivas del alumno via inscripciones (no tienen idClase en el modelo). */
    private List<AsignaturaResponseDTO> electivasDelEstudiante(Long idEstudiante) {
        return contentOf(asignaturaClient.searchInscripcionesByAlumno(idEstudiante, PAGE_SIZE)).stream()
            .filter(inscripcion -> inscripcion.estado() != EstadoInscripcion.CANCELADO)
            .map(InscripcionResponseDTO::idAsignatura)
            .filter(Objects::nonNull)
            .distinct()
            .map(asignaturaClient::getAsignaturaById)
            .filter(Objects::nonNull)
            .toList();
    }

    private static List<AsignaturaResponseDTO> unirAsignaturas(
            List<AsignaturaResponseDTO> basicas,
            List<AsignaturaResponseDTO> electivas) {
        Map<Long, AsignaturaResponseDTO> unicas = new LinkedHashMap<>();
        basicas.forEach(asignatura -> unicas.putIfAbsent(asignatura.id(), asignatura));
        electivas.forEach(asignatura -> unicas.putIfAbsent(asignatura.id(), asignatura));
        return List.copyOf(unicas.values());
    }

    private Map<Long, String> docentePorId(List<AsignaturaResponseDTO> asignaturas) {
        Map<Long, String> docentes = new HashMap<>();
        asignaturas.stream()
            .map(AsignaturaResponseDTO::idDocente)
            .filter(id -> id != null && !docentes.containsKey(id))
            .forEach(id -> {
                DocenteResponseDTO docente = docenteClient.getDocenteById(id);
                docentes.put(id, docente == null ? null : nombreDocente(docente));
            });
        return docentes;
    }

    private static String nombreDocente(DocenteResponseDTO docente) {
        return Stream.of(docente.firstName(), docente.middleName(), docente.firstSurname(), docente.secondSurname())
            .filter(parte -> parte != null && !parte.isBlank())
            .collect(Collectors.joining(" "));
    }

    private Map<Long, Double> notasPorEvaluacion(Long idEstudiante) {
        return contentOf(notaClient.searchNotas(idEstudiante, PAGE_SIZE)).stream()
            .filter(nota -> nota.idEvaluacion() != null)
            .collect(Collectors.toMap(NotaResponseDTO::idEvaluacion, NotaResponseDTO::score, (a, b) -> a));
    }

    private static <T> List<T> contentOf(PageResponseDTO<T> pagina) {
        return pagina == null || pagina.content() == null ? Collections.emptyList() : pagina.content();
    }

    private AsignaturaDetalleDTO toDetalle(
            AsignaturaResponseDTO asignatura,
            Map<Long, Double> notaPorEvaluacion,
            Map<Long, String> docentePorId) {
        List<HorarioDetalleDTO> horarios = asignatura.horarios() == null
            ? Collections.emptyList()
            : asignatura.horarios().stream()
                .map(horario -> new HorarioDetalleDTO(
                    horario.id(),
                    horario.dia() == null ? null : horario.dia().getNombre(),
                    horario.horarioEntrada() == null ? null : horario.horarioEntrada().toString(),
                    horario.horarioSalida() == null ? null : horario.horarioSalida().toString(),
                    horario.ubicacion()))
                .toList();

        List<EvaluacionDetalleDTO> evaluaciones = contentOf(
                evaluacionClient.searchEvaluacionesByAsignatura(asignatura.id(), PAGE_SIZE)).stream()
            .map(evaluacion -> new EvaluacionDetalleDTO(
                evaluacion.id(),
                evaluacion.nombre(),
                evaluacion.tipo() == null ? null : evaluacion.tipo().name(),
                evaluacion.ponderacion(),
                notaPorEvaluacion.get(evaluacion.id())))
            .toList();

        return new AsignaturaDetalleDTO(
            asignatura.id(),
            asignatura.name(),
            asignatura.description(),
            asignatura.idDocente(),
            docentePorId.get(asignatura.idDocente()),
            horarios,
            evaluaciones);
    }
}

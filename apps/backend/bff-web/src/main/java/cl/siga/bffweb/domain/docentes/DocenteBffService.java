package cl.siga.bffweb.domain.docentes;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import cl.siga.bffweb.domain.docentes.dto.AlumnoDTO;
import cl.siga.bffweb.domain.docentes.dto.ClaseDocenteDTO;
import cl.siga.bffweb.domain.docentes.dto.CursoDocenteDTO;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.security.SecurityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Cursos y horario del docente autenticado: asignaturas (ms-asignaturas) ->
 * clases (ms-clases) -> alumnos (ms-estudiantes).
 */
@Service
@RequiredArgsConstructor
public class DocenteBffService {
    private static final int PAGE_SIZE = 200;
    private static final List<String> ORDEN_DIAS =
        List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");

    private final DocenteClient docenteClient;
    private final AsignaturaClient asignaturaClient;
    private final ClaseClient claseClient;
    private final EstudianteClient estudianteClient;

    public List<CursoDocenteDTO> getCursos() {
        DocenteResponseDTO docente = docenteActual();
        return asignaturasDelDocente(docente.id()).stream()
            .filter(asignatura -> asignatura.idClase() != null)
            .map(asignatura -> toCurso(docente.id(), asignatura))
            .toList();
    }

    public List<ClaseDocenteDTO> getHorario() {
        DocenteResponseDTO docente = docenteActual();
        return asignaturasDelDocente(docente.id()).stream()
            .flatMap(asignatura -> toClases(asignatura).stream())
            .sorted(Comparator
                .comparingInt((ClaseDocenteDTO clase) -> ORDEN_DIAS.indexOf(clase.dia()))
                .thenComparingInt(ClaseDocenteDTO::franja))
            .toList();
    }

    private CursoDocenteDTO toCurso(Long docenteId, AsignaturaResponseDTO asignatura) {
        ClaseResponseDTO clase = claseClient.getClaseById(asignatura.idClase());
        List<AlumnoDTO> alumnos = contentOf(
                estudianteClient.searchEstudiantesByClase(asignatura.idClase(), PAGE_SIZE)).stream()
            .map(estudiante -> new AlumnoDTO(
                estudiante.id(), nombres(estudiante),
                estudiante.firstSurname(), estudiante.secondSurname()))
            .toList();
        return new CursoDocenteDTO(
            asignatura.id(),
            nombreClase(clase),
            clase.nivel().ordinal() + 1,
            clase.letra(),
            asignatura.name(),
            docenteId,
            sala(asignatura, clase),
            diasClase(asignatura),
            alumnos);
    }

    private List<ClaseDocenteDTO> toClases(AsignaturaResponseDTO asignatura) {
        ClaseResponseDTO clase = asignatura.idClase() == null
            ? null
            : claseClient.getClaseById(asignatura.idClase());
        String curso = clase == null ? asignatura.name() : nombreClase(clase);
        return (asignatura.horarios() == null ? Collections.<HorarioResponseDTO>emptyList() : asignatura.horarios())
            .stream()
            .map(horario -> new ClaseDocenteDTO(
                franja(horario.horarioEntrada()),
                asignatura.id(),
                curso,
                asignatura.name(),
                horario.ubicacion(),
                horario.dia() == null ? null : horario.dia().getNombre(),
                horario.horarioEntrada() == null ? null : horario.horarioEntrada().toString(),
                horario.horarioSalida() == null ? null : horario.horarioSalida().toString()))
            .toList();
    }

    private DocenteResponseDTO docenteActual() {
        String oid = SecurityUtils.getCurrentUserOid()
            .orElseThrow(() -> new BusinessException("No se pudo determinar el usuario autenticado."));
        return docenteClient.getDocenteByIdUsuario(oid);
    }

    private List<AsignaturaResponseDTO> asignaturasDelDocente(Long idDocente) {
        return contentOf(asignaturaClient.searchAsignaturasByDocente(idDocente, PAGE_SIZE));
    }

    private static String nombres(EstudianteResponseDTO estudiante) {
        return Stream.of(estudiante.firstName(), estudiante.middleName())
            .filter(parte -> parte != null && !parte.isBlank())
            .collect(Collectors.joining(" "));
    }

    private static String nombreClase(ClaseResponseDTO clase) {
        return clase.nivel().getDescripcion() + " " + clase.letra();
    }

    private static String sala(AsignaturaResponseDTO asignatura, ClaseResponseDTO clase) {
        if (asignatura.horarios() != null && !asignatura.horarios().isEmpty()) {
            return asignatura.horarios().get(0).ubicacion();
        }
        return "Sala " + clase.nivel().getDescripcion() + " " + clase.letra();
    }

    private static List<String> diasClase(AsignaturaResponseDTO asignatura) {
        if (asignatura.horarios() == null) {
            return List.of();
        }
        return asignatura.horarios().stream()
            .map(horario -> horario.dia() == null ? null : horario.dia().getNombre())
            .filter(dia -> dia != null)
            .distinct()
            .sorted(Comparator.comparingInt(ORDEN_DIAS::indexOf))
            .toList();
    }

    /** Franja (bloque doble) a partir de la hora de entrada de 45 minutos. */
    private static int franja(LocalTime hora) {
        if (hora == null) {
            return 0;
        }
        return switch (hora.toString()) {
            case "08:00", "08:45" -> 1;
            case "09:50", "10:35" -> 2;
            case "12:15", "13:00" -> 3;
            default -> 4;
        };
    }

    private static <T> List<T> contentOf(PageResponseDTO<T> pagina) {
        return pagina == null || pagina.content() == null ? List.of() : pagina.content();
    }
}

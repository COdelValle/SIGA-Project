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
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import lombok.RequiredArgsConstructor;

/**
 * Cursos y horario del docente autenticado: dictaciones (ms-asignaturas) ->
 * clases (ms-clases) -> alumnos (ms-estudiantes).
 */
@Service
@RequiredArgsConstructor
public class DocenteBffService {
    private static final int PAGE_SIZE = 100;
    private static final List<String> ORDEN_DIAS =
        List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");

    private final DocenteContextService docenteContext;
    private final ClaseClient claseClient;
    private final EstudianteClient estudianteClient;

    public List<CursoDocenteDTO> getCursos() {
        DocenteResponseDTO docente = docenteContext.docenteActual();
        return docenteContext.cursosDelDocente(docente.id()).stream()
            .map(curso -> toCurso(docente.id(), curso))
            .toList();
    }

    public List<ClaseDocenteDTO> getHorario() {
        DocenteResponseDTO docente = docenteContext.docenteActual();
        return docenteContext.cursosDelDocente(docente.id()).stream()
            .flatMap(curso -> toClases(curso).stream())
            .sorted(Comparator
                .comparingInt((ClaseDocenteDTO clase) -> ORDEN_DIAS.indexOf(clase.dia()))
                .thenComparingInt(ClaseDocenteDTO::franja))
            .toList();
    }

    private CursoDocenteDTO toCurso(Long docenteId, CursoAsignaturaResponseDTO curso) {
        ClaseResponseDTO clase = claseClient.getClaseById(curso.idClase());
        List<AlumnoDTO> alumnos = contentOf(
                estudianteClient.searchEstudiantesByClase(curso.idClase(), PAGE_SIZE)).stream()
            .map(estudiante -> new AlumnoDTO(
                estudiante.id(), nombres(estudiante),
                estudiante.firstSurname(), estudiante.secondSurname()))
            .toList();
        return new CursoDocenteDTO(
            curso.id(),
            curso.idAsignatura(),
            nombreClase(clase),
            clase.nivel().ordinal() + 1,
            clase.letra(),
            curso.nombre(),
            docenteId,
            sala(curso, clase),
            diasClase(curso),
            alumnos);
    }

    private List<ClaseDocenteDTO> toClases(CursoAsignaturaResponseDTO curso) {
        ClaseResponseDTO clase = curso.idClase() == null
            ? null
            : claseClient.getClaseById(curso.idClase());
        String nombreCurso = clase == null ? curso.nombre() : nombreClase(clase);
        return (curso.horarios() == null ? Collections.<HorarioResponseDTO>emptyList() : curso.horarios())
            .stream()
            .map(horario -> new ClaseDocenteDTO(
                franja(horario.horarioEntrada()),
                curso.id(),
                nombreCurso,
                curso.nombre(),
                horario.ubicacion(),
                horario.dia() == null ? null : horario.dia().getNombre(),
                horario.horarioEntrada() == null ? null : horario.horarioEntrada().toString(),
                horario.horarioSalida() == null ? null : horario.horarioSalida().toString()))
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

    private static String sala(CursoAsignaturaResponseDTO curso, ClaseResponseDTO clase) {
        if (curso.horarios() != null && !curso.horarios().isEmpty()) {
            return curso.horarios().get(0).ubicacion();
        }
        return "Sala " + clase.nivel().getDescripcion() + " " + clase.letra();
    }

    private static List<String> diasClase(CursoAsignaturaResponseDTO curso) {
        if (curso.horarios() == null) {
            return List.of();
        }
        return curso.horarios().stream()
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

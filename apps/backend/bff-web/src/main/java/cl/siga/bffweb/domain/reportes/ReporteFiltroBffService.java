package cl.siga.bffweb.domain.reportes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import cl.siga.bffweb.domain.docentes.DocenteContextService;
import cl.siga.bffweb.domain.reportes.dto.CursoReporteDTO;
import cl.siga.bffweb.domain.reportes.dto.EstudianteReporteDTO;
import cl.siga.bffweb.domain.reportes.dto.ReporteFiltrosDTO;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.security.SecurityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Entrega los cursos y estudiantes que puede filtrar el dashboard de reportes
 * según el rol autenticado. El docente solo ve sus dictaciones; el
 * administrador ve todas.
 */
@Service
@RequiredArgsConstructor
public class ReporteFiltroBffService {

    private static final int PAGE_SIZE = 500;

    private final AsignaturaClient asignaturaClient;
    private final ClaseClient claseClient;
    private final EstudianteClient estudianteClient;
    private final DocenteContextService docenteContext;

    public ReporteFiltrosDTO filtros() {
        List<CursoAsignaturaResponseDTO> cursos = cursosVisibles();
        Map<Long, String> nombresClase = new HashMap<>();
        List<CursoReporteDTO> opciones = cursos.stream()
            .map(curso -> new CursoReporteDTO(
                curso.id(),
                curso.nombre(),
                nombreClase(curso, nombresClase),
                curso.idClase()))
            .toList();
        return new ReporteFiltrosDTO(opciones);
    }

    public List<EstudianteReporteDTO> estudiantes(Long idCursoAsignatura) {
        CursoAsignaturaResponseDTO curso = asignaturaClient.getCursoAsignaturaById(idCursoAsignatura);
        if (curso == null) {
            throw new cl.siga.coreshare.exception.ResourceNotFoundException(
                "Dictación con ID " + idCursoAsignatura + " no encontrada.");
        }
        if (SecurityUtils.hasRole("DOCENTE")) {
            docenteContext.validarCursoDelDocente(idCursoAsignatura);
        } else if (!SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("No tienes permiso para consultar los estudiantes del curso.");
        }
        return contentOf(estudianteClient.searchEstudiantesByClase(curso.idClase(), PAGE_SIZE)).stream()
            .map(this::toEstudiante)
            .toList();
    }

    private List<CursoAsignaturaResponseDTO> cursosVisibles() {
        if (SecurityUtils.isAdmin()) {
            return contentOf(asignaturaClient.searchCursoAsignaturas(null, null, PAGE_SIZE));
        }
        if (SecurityUtils.hasRole("DOCENTE")) {
            return docenteContext.cursosDelDocente(docenteContext.docenteActual().id());
        }
        throw new AccessDeniedException("No tienes permiso para consultar los filtros de reportes.");
    }

    private String nombreClase(CursoAsignaturaResponseDTO curso, Map<Long, String> cache) {
        if (curso.idClase() == null) {
            return curso.nombre();
        }
        return cache.computeIfAbsent(curso.idClase(), id -> {
            try {
                ClaseResponseDTO clase = claseClient.getClaseById(id);
                if (clase == null || clase.nivel() == null) {
                    return "Curso #" + id;
                }
                return clase.nivel().getDescripcion() + " " + clase.letra();
            } catch (RuntimeException ex) {
                return "Curso #" + id;
            }
        });
    }

    private EstudianteReporteDTO toEstudiante(EstudianteResponseDTO estudiante) {
        String nombre = Stream.of(
                estudiante.firstName(),
                estudiante.middleName(),
                estudiante.firstSurname(),
                estudiante.secondSurname())
            .filter(parte -> parte != null && !parte.isBlank())
            .reduce((a, b) -> a + " " + b)
            .orElse("Estudiante " + estudiante.id());
        return new EstudianteReporteDTO(estudiante.id(), nombre, estudiante.rut());
    }

    private static <T> List<T> contentOf(PageResponseDTO<T> pagina) {
        return pagina == null || pagina.content() == null ? List.of() : pagina.content();
    }
}

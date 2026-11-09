package cl.siga.bffweb.domain.reportes;

import cl.siga.bffweb.domain.docentes.DocenteContextService;
import cl.siga.bffweb.integration.apoderados.ApoderadoClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.bffweb.integration.reportes.ReporteClient;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.reportes.IndicadorAsistenciaDTO;
import cl.siga.coreshare.dto.reportes.IndicadorRendimientoDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.security.SecurityUtils;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReporteBffService {

    private final ReporteClient reporteClient;
    private final EstudianteClient estudianteClient;
    private final ApoderadoClient apoderadoClient;
    private final DocenteContextService docenteContext;

    public List<IndicadorAsistenciaDTO> buscarAsistencia(
            Long estudiante, Long curso, LocalDate from, LocalDate to) {
        Long idEstudiante = resolverEstudiante(estudiante, curso);
        validarCurso(curso);
        return reporteClient.buscarAsistencia(idEstudiante, curso, from, to);
    }

    public List<IndicadorRendimientoDTO> buscarRendimiento(
            Long estudiante, Long curso, LocalDate from, LocalDate to) {
        Long idEstudiante = resolverEstudiante(estudiante, curso);
        validarCurso(curso);
        return reporteClient.buscarRendimiento(idEstudiante, curso, from, to);
    }

    private Long resolverEstudiante(Long solicitado, Long curso) {
        if (SecurityUtils.isAdmin()) {
            return solicitado;
        }
        if (SecurityUtils.hasRole("DOCENTE")) {
            if (curso == null) {
                throw new AccessDeniedException("Un docente debe filtrar por curso.");
            }
            return solicitado;
        }
        if (solicitado == null) {
            if (!SecurityUtils.hasRole("ESTUDIANTE")) {
                throw new AccessDeniedException("Debes indicar un estudiante.");
            }
            EstudianteResponseDTO propio = estudianteClient.getEstudianteByIdUsuario(oid());
            return propio.id();
        }
        EstudianteResponseDTO estudiante = estudianteClient.getEstudianteById(solicitado);
        if (SecurityUtils.hasRole("ESTUDIANTE") && oid().equals(estudiante.idUsuario())) {
            return solicitado;
        }
        if (SecurityUtils.hasRole("APODERADO")) {
            ApoderadoResponseDTO apoderado = apoderadoClient.getApoderadoByIdUsuario(oid());
            boolean vinculado = apoderado.estudiantes() != null
                && apoderado.estudiantes().stream()
                    .anyMatch(vinculo -> solicitado.equals(vinculo.idEstudiante()));
            if (vinculado) {
                return solicitado;
            }
        }
        throw new AccessDeniedException("No tienes permiso para consultar este estudiante.");
    }

    private void validarCurso(Long curso) {
        if (curso != null && SecurityUtils.hasRole("DOCENTE")) {
            docenteContext.validarCursoDelDocente(curso);
        }
    }

    private String oid() {
        return SecurityUtils.getCurrentUserOid()
            .orElseThrow(() -> new BusinessException("No se pudo determinar el usuario autenticado."));
    }
}

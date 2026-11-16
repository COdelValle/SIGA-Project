package cl.siga.bffweb.domain.reportes;

import cl.siga.bffweb.domain.reportes.dto.EstudianteReporteDTO;
import cl.siga.bffweb.domain.reportes.dto.ReporteFiltrosDTO;
import cl.siga.coreshare.dto.reportes.IndicadorAsistenciaDTO;
import cl.siga.coreshare.dto.reportes.IndicadorRendimientoDTO;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bff/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteBffService service;
    private final ReporteFiltroBffService filtroService;

    @GetMapping("/filtros")
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) "
        + "and hasAuthority('SCOPE_asistencias:read') and hasAuthority('SCOPE_notas:read')")
    public ResponseEntity<ReporteFiltrosDTO> filtros() {
        return ResponseEntity.ok(filtroService.filtros());
    }

    @GetMapping("/filtros/estudiantes")
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) "
        + "and hasAuthority('SCOPE_asistencias:read') and hasAuthority('SCOPE_notas:read')")
    public ResponseEntity<List<EstudianteReporteDTO>> estudiantes(
            @RequestParam Long idCursoAsignatura) {
        return ResponseEntity.ok(filtroService.estudiantes(idCursoAsignatura));
    }

    @GetMapping("/asistencia")
    @PreAuthorize("hasAuthority('SCOPE_asistencias:read')")
    public ResponseEntity<List<IndicadorAsistenciaDTO>> asistencia(
            @RequestParam(required = false) Long idEstudiante,
            @RequestParam(required = false) Long idCursoAsignatura,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.buscarAsistencia(idEstudiante, idCursoAsignatura, from, to));
    }

    @GetMapping("/rendimiento")
    @PreAuthorize("hasAuthority('SCOPE_notas:read')")
    public ResponseEntity<List<IndicadorRendimientoDTO>> rendimiento(
            @RequestParam(required = false) Long idEstudiante,
            @RequestParam(required = false) Long idCursoAsignatura,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.buscarRendimiento(idEstudiante, idCursoAsignatura, from, to));
    }
}

package cl.siga.msreportes.controller;

import cl.siga.coreshare.dto.reportes.IndicadorAsistenciaDTO;
import cl.siga.coreshare.dto.reportes.IndicadorRendimientoDTO;
import cl.siga.msreportes.service.ReporteConsultaService;
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
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteConsultaService consultaService;

    @GetMapping("/asistencia")
    @PreAuthorize("hasAuthority('SCOPE_asistencias:read')")
    public ResponseEntity<List<IndicadorAsistenciaDTO>> asistencia(
            @RequestParam(required = false) Long idEstudiante,
            @RequestParam(required = false) Long idCursoAsignatura,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(consultaService.buscarAsistencia(idEstudiante, idCursoAsignatura, from, to));
    }

    @GetMapping("/rendimiento")
    @PreAuthorize("hasAuthority('SCOPE_notas:read')")
    public ResponseEntity<List<IndicadorRendimientoDTO>> rendimiento(
            @RequestParam(required = false) Long idEstudiante,
            @RequestParam(required = false) Long idCursoAsignatura,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(consultaService.buscarRendimiento(idEstudiante, idCursoAsignatura, from, to));
    }
}

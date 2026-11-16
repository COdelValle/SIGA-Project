package cl.siga.bffweb.integration.reportes;

import cl.siga.coreshare.dto.reportes.IndicadorAsistenciaDTO;
import cl.siga.coreshare.dto.reportes.IndicadorRendimientoDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "ms-reportes",
    url = "${services.reportes.url}",
    fallbackFactory = ReporteClientFallbackFactory.class
)
public interface ReporteClient {

    @GetMapping("/api/v1/reportes/asistencia")
    List<IndicadorAsistenciaDTO> buscarAsistencia(
        @RequestParam(value = "idEstudiante", required = false) Long idEstudiante,
        @RequestParam(value = "idCursoAsignatura", required = false) Long idCursoAsignatura,
        @RequestParam(value = "from", required = false) LocalDate from,
        @RequestParam(value = "to", required = false) LocalDate to);

    @GetMapping("/api/v1/reportes/rendimiento")
    List<IndicadorRendimientoDTO> buscarRendimiento(
        @RequestParam(value = "idEstudiante", required = false) Long idEstudiante,
        @RequestParam(value = "idCursoAsignatura", required = false) Long idCursoAsignatura,
        @RequestParam(value = "from", required = false) LocalDate from,
        @RequestParam(value = "to", required = false) LocalDate to);
}

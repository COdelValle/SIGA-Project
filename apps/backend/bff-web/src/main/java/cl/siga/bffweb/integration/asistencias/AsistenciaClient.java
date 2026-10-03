package cl.siga.bffweb.integration.asistencias;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;

@FeignClient (
    name = "ms-asistencias",
    url = "${services.asistencias.url}",
    fallback = AsistenciaClientFallback.class
)
public interface AsistenciaClient {
    @GetMapping ("/api/v1/asistencias/search")
    PageResponseDTO<AsistenciaResponseDTO> searchAsistencias(
        @RequestParam ("idEstudiante") Long idEstudiante,
        @RequestParam ("size") int size);

    @PostMapping ("/api/v1/asistencias")
    AsistenciaResponseDTO saveAsistencia(@RequestBody RegistrarAsistenciaRequestDTO request);

    @PutMapping ("/api/v1/asistencias/{id}")
    AsistenciaResponseDTO updateAsistencia(
        @PathVariable ("id") Long id,
        @RequestBody ActualizarAsistenciaRequestDTO request);
}

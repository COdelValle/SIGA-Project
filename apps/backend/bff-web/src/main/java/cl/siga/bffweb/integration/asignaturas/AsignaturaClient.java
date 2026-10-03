package cl.siga.bffweb.integration.asignaturas;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;

@FeignClient (
    name = "ms-asignaturas",
    url = "${services.asignaturas.url}",
    fallback = AsignaturaClientFallback.class
)
public interface AsignaturaClient {
    @GetMapping ("/api/v1/asignaturas/{id}")
    AsignaturaResponseDTO getAsignaturaById(@PathVariable ("id") Long id);

    @GetMapping ("/api/v1/asignaturas/search")
    PageResponseDTO<AsignaturaResponseDTO> searchAsignaturasByClase(
        @RequestParam ("idClase") Long idClase,
        @RequestParam ("size") int size);
}

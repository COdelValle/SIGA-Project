package cl.siga.bffweb.integration.clases;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;

@FeignClient (
    name = "ms-clases",
    url = "${services.clases.url}",
    fallback = ClaseClientFallback.class
)
public interface ClaseClient {
    @GetMapping ("/api/v1/clases/{id}")
    ClaseResponseDTO getClaseById(@PathVariable ("id") Long id);

    @GetMapping ("/api/v1/clases/search")
    PageResponseDTO<ClaseResponseDTO> searchClases(
        @RequestParam ("anioAcademico") Integer anioAcademico,
        @RequestParam ("size") int size);
}

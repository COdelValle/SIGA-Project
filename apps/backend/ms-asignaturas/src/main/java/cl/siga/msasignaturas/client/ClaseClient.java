package cl.siga.msasignaturas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.coreshare.dto.clase.ClaseResponseDTO;

@FeignClient (
    name = "ms-clases",
    url = "${services.ms-clases.url}",
    fallbackFactory = ClaseClientFallbackFactory.class
)
public interface ClaseClient {
    @GetMapping ("/api/v1/clases/exists/{id}")
    boolean existsById(@PathVariable ("id") Long id);

    @GetMapping ("/api/v1/clases/{id}")
    ClaseResponseDTO getClaseById(@PathVariable ("id") Long id);
}

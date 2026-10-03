package cl.siga.bffweb.integration.docentes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.coreshare.dto.docente.DocenteResponseDTO;

@FeignClient (
    name = "ms-docentes",
    url = "${services.docentes.url}",
    fallback = DocenteClientFallback.class
)
public interface DocenteClient {
    @GetMapping ("/api/v1/docentes/{id}")
    DocenteResponseDTO getDocenteById(@PathVariable ("id") Long id);
}

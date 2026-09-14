package cl.siga.bffweb.integration.asignaturas;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.bffweb.config.FeignClientConfig;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;

@FeignClient (
    name = "ms-asignaturas",
    url = "${microservices.asignaturas.url}",
    configuration = FeignClientConfig.class,
    fallback = AsignaturaClientFallback.class
)
public interface AsignaturaClient {
    @GetMapping ("/api/v1/asignaturas/{id}")
    AsignaturaResponseDTO getAsignaturaById(@PathVariable Long id);
}

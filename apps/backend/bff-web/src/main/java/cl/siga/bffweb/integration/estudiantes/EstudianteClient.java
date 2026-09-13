package cl.siga.bffweb.integration.estudiantes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.bffweb.config.FeignClientConfig;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;

@FeignClient (
    name = "ms-estudiantes",
    url = "${microservices.estudiantes.url}",
    configuration = FeignClientConfig.class,
    fallback = EstudianteClientFallback.class
)
public interface EstudianteClient {
    @GetMapping ("/api/v1/estudiantes/{id}")
    EstudianteResponseDTO getEstudianteById(@PathVariable String id);
}

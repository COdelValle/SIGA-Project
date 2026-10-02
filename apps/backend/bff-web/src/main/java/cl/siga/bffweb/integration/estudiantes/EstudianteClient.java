package cl.siga.bffweb.integration.estudiantes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;

@FeignClient (
    name = "ms-estudiantes",
    url = "${services.estudiantes.url}",
    fallback = EstudianteClientFallback.class
)
public interface EstudianteClient {
    @GetMapping ("/api/v1/estudiantes/{id}")
    EstudianteResponseDTO getEstudianteById(@PathVariable String id);
}

package cl.siga.bffweb.integration.evaluaciones;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;

@FeignClient (
    name = "ms-evaluaciones",
    url = "${services.evaluaciones.url}",
    fallback = EvaluacionClientFallback.class
)
public interface EvaluacionClient {
    @GetMapping ("/api/v1/evaluaciones/{id}")
    EvaluacionResponseDTO getEvaluacionById(@PathVariable Long id);
}

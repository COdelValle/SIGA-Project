package cl.siga.bffweb.integration.evaluaciones;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.bffweb.config.FeignClientConfig;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;

@FeignClient (
    name = "ms-evaluaciones",
    url = "${microservices.evaluaciones.url}",
    configuration = FeignClientConfig.class,
    fallback = EvaluacionClientFallback.class
)
public interface EvaluacionClient {
    @GetMapping ("/api/v1/evaluaciones/{id}")
    EvaluacionResponseDTO getEvaluacionById(@PathVariable Long id);
}

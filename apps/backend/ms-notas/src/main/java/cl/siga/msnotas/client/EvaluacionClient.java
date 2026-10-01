package cl.siga.msnotas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "ms-evaluacion",
    url = "${services.ms-evaluacion.url}",
    fallback = EvaluacionClientFallback.class
)
public interface EvaluacionClient {

    @GetMapping("/api/v1/evaluacion/exists/{id}")
    boolean existsById(@PathVariable("id") Long id);
}

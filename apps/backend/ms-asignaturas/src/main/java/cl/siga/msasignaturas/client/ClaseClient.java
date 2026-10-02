package cl.siga.msasignaturas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.msasignaturas.config.FeignAuthConfig;

@FeignClient (
    name = "ms-clases",
    url = "${services.ms-clases.url}",
    configuration = FeignAuthConfig.class,
    fallback = ClaseClientFallback.class
)
public interface ClaseClient {
    @GetMapping ("/api/v1/clases/exists/{id}")
    boolean existsById(@PathVariable ("id") Long id);
}

package cl.siga.msasistencias.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "ms-asignaturas",
    url = "${services.ms-asignaturas.url}",
    fallbackFactory = AsignaturaClientFallbackFactory.class
)
public interface AsignaturaClient {

    @GetMapping("/api/v1/asignaturas/exists/{id}")
    boolean existsById(@PathVariable("id") Long id);
}

package cl.siga.msasignaturas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "ms-docentes",
    url = "${services.ms-docentes.url}",
    fallbackFactory = DocenteClientFallbackFactory.class
)
public interface DocenteClient {

    @GetMapping("/api/v1/docentes/exists/{id}")
    boolean existsById(@PathVariable("id") Long id);
}

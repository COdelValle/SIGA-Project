package cl.siga.msnotas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "ms-estudiantes",
    url = "${services.ms-estudiantes.url}",
    fallback = EstudianteClientFallback.class
)
public interface EstudianteClient {

    @GetMapping("/api/v1/estudiantes/exists/{id}")
    boolean existsById(@PathVariable("id") Long id);
}

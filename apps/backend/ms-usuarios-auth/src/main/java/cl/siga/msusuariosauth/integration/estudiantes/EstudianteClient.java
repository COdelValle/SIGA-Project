package cl.siga.msusuariosauth.integration.estudiantes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Validación síncrona de estudiantes durante el alta de apoderados. El token
 * del administrador se propaga con {@code SharedFeignAuthConfig}.
 */
@FeignClient(name = "ms-estudiantes", url = "${services.estudiantes.url:http://localhost:8083}")
public interface EstudianteClient {

    @GetMapping("/api/v1/estudiantes/exists/{id}")
    Boolean existsById(@PathVariable("id") Long id);
}

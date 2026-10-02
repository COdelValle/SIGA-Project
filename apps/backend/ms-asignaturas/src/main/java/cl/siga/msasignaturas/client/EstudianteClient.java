package cl.siga.msasignaturas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.msasignaturas.config.FeignAuthConfig;

@FeignClient (
    name = "ms-estudiantes",
    url = "${services.ms-estudiantes.url}",
    configuration = FeignAuthConfig.class,
    fallback = EstudianteClientFallback.class
)
public interface EstudianteClient {
    @GetMapping ("/api/v1/estudiantes/idUsuario/{idUsuario}")
    EstudianteResponseDTO getEstudianteByIdUsuario(@PathVariable ("idUsuario") String idUsuario);
}

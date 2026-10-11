package cl.siga.bffweb.integration.estudiantes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;

@FeignClient (
    name = "ms-estudiantes",
    url = "${services.estudiantes.url}",
    fallback = EstudianteClientFallback.class
)
public interface EstudianteClient {
    @GetMapping ("/api/v1/estudiantes/{id}")
    EstudianteResponseDTO getEstudianteById(@PathVariable ("id") Long id);

    @GetMapping ("/api/v1/estudiantes/idUsuario/{idUsuario}")
    EstudianteResponseDTO getEstudianteByIdUsuario(@PathVariable ("idUsuario") String idUsuario);

    @GetMapping ("/api/v1/estudiantes/search")
    PageResponseDTO<EstudianteResponseDTO> searchEstudiantesByClase(
        @RequestParam ("idClase") Long idClase,
        @RequestParam ("size") int size);

    @GetMapping ("/api/v1/estudiantes/search")
    PageResponseDTO<EstudianteResponseDTO> searchEstudiantes(
        @RequestParam ("q") String q,
        @RequestParam ("size") int size);

    @PutMapping ("/api/v1/estudiantes/{id}")
    EstudianteResponseDTO updateEstudiante(
        @PathVariable ("id") Long id,
        @RequestBody ActualizarEstudianteRequestDTO request);
}

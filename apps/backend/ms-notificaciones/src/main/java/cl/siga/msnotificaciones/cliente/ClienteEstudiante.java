package cl.siga.msnotificaciones.cliente;

import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.msnotificaciones.cliente.respaldo.RespaldoEstudianteFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Resuelve el nombre del estudiante desde su idAlumno.
 */
@FeignClient(
    name = "ms-estudiantes",
    url = "${servicios.ms-estudiantes.url}",
    fallbackFactory = RespaldoEstudianteFactory.class
)
public interface ClienteEstudiante {

    @GetMapping("/api/v1/estudiantes/{id}")
    EstudianteResponseDTO obtenerPorId(@PathVariable("id") Long id);
}

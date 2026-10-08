package cl.siga.msnotificaciones.cliente;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.msnotificaciones.cliente.respaldo.RespaldoInscripcionFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Resuelve dictaciones activas del perfil académico en la consulta autenticada.
 */
@FeignClient(
    name = "ms-asignaturas-inscripciones",
    url = "${services.ms-asignaturas.url}",
    fallbackFactory = RespaldoInscripcionFactory.class
)
public interface ClienteInscripcion {

    @GetMapping("/api/v1/inscripciones/search")
    PageResponseDTO<InscripcionResponseDTO> buscarPorDictacion(
        @RequestParam("idCursoAsignatura") Long idCursoAsignatura,
        @RequestParam("size") int tamano);

    @GetMapping("/api/v1/inscripciones/search")
    PageResponseDTO<InscripcionResponseDTO> buscarPorEstudiante(
        @RequestParam("idAlumno") Long idEstudiante,
        @RequestParam("size") int tamano);
}

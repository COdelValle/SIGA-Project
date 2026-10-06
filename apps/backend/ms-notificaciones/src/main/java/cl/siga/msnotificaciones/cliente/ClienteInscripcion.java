package cl.siga.msnotificaciones.cliente;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.msnotificaciones.cliente.respaldo.RespaldoInscripcionFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Pide la lista de inscritos a la dictación.
 * Esos idAlumno son los estudiantes que deben enterarse.
 * Contrato real: GET /api/v1/inscripciones/search -> Page.
 */
@FeignClient(
    name = "ms-asignaturas-inscripciones",
    url = "${servicios.ms-asignaturas.url}",
    fallbackFactory = RespaldoInscripcionFactory.class
)
public interface ClienteInscripcion {

    @GetMapping("/api/v1/inscripciones/search")
    PageResponseDTO<InscripcionResponseDTO> buscarPorDictacion(
        @RequestParam("idCursoAsignatura") Long idCursoAsignatura,
        @RequestParam("size") int tamano);
}

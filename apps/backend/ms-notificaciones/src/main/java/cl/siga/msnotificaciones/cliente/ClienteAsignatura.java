package cl.siga.msnotificaciones.cliente;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.msnotificaciones.cliente.respaldo.RespaldoAsignaturaFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Consulta las dictaciones del curso para autorizar la visibilidad de eventos de evaluación.
 */
@FeignClient(
    name = "ms-asignaturas",
    url = "${services.ms-asignaturas.url}",
    fallbackFactory = RespaldoAsignaturaFactory.class
)
public interface ClienteAsignatura {

    @GetMapping("/api/v1/curso-asignaturas/{id}")
    CursoAsignaturaResponseDTO obtenerDictacion(@PathVariable("id") Long id);

    @GetMapping("/api/v1/curso-asignaturas/search")
    PageResponseDTO<CursoAsignaturaResponseDTO> buscarPorClase(
        @RequestParam("idClase") Long idClase,
        @RequestParam("size") int tamano);
}

package cl.siga.msnotificaciones.cliente;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.msnotificaciones.cliente.respaldo.RespaldoAsignaturaFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Pregunta a ms-asignaturas cómo se llama la dictación, para armar un mensaje lindo ("Prueba 1 de Matemática 4°B").
 */
@FeignClient(
    name = "ms-asignaturas",
    url = "${servicios.ms-asignaturas.url}",
    fallbackFactory = RespaldoAsignaturaFactory.class
)
public interface ClienteAsignatura {

    @GetMapping("/api/v1/curso-asignaturas/{id}")
    CursoAsignaturaResponseDTO obtenerDictacion(@PathVariable("id") Long id);
}

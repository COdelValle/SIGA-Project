package cl.siga.msnotas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;

@FeignClient(
    name = "ms-asignaturas",
    url = "${services.ms-asignaturas.url}",
    fallbackFactory = AsignaturaClientFallbackFactory.class
)
public interface AsignaturaClient {

    /** Nombre de la asignatura de la dictacion, para enriquecer la notificacion. */
    @GetMapping("/api/v1/curso-asignaturas/{id}")
    CursoAsignaturaResponseDTO getCursoAsignaturaById(@PathVariable("id") Long id);
}

package cl.siga.msnotificaciones.cliente;

import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.msnotificaciones.cliente.respaldo.RespaldoApoderadoFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Resuelve el perfil del apoderado autenticado y los vínculos con pupilos.
 */
@FeignClient(
    name = "ms-apoderados",
    url = "${services.ms-apoderados.url}",
    fallbackFactory = RespaldoApoderadoFactory.class
)
public interface ClienteApoderado {

    @GetMapping("/api/v1/apoderados/idUsuario/{idUsuario}")
    ApoderadoResponseDTO obtenerPorIdUsuario(@PathVariable("idUsuario") String idUsuario);

    @GetMapping("/api/v1/apoderados/search")
    PageResponseDTO<ApoderadoResponseDTO> buscarPorEstudiante(
        @RequestParam("idEstudiante") Long idEstudiante,
        @RequestParam("size") int tamano);
}

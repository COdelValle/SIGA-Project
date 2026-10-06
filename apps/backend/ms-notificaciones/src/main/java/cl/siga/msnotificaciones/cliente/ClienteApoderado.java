package cl.siga.msnotificaciones.cliente;

import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.msnotificaciones.cliente.respaldo.RespaldoApoderadoFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Busca apoderados vinculados a un estudiante, para avisarle también a la familia.
 * Contrato real: GET /api/v1/apoderados/search?idEstudiante=&page=&size= -> Page.
 */
@FeignClient(
    name = "ms-apoderados",
    url = "${servicios.ms-apoderados.url}",
    fallbackFactory = RespaldoApoderadoFactory.class
)
public interface ClienteApoderado {

    @GetMapping("/api/v1/apoderados/search")
    PageResponseDTO<ApoderadoResponseDTO> buscarPorEstudiante(
        @RequestParam("idEstudiante") Long idEstudiante,
        @RequestParam("size") int tamano);
}

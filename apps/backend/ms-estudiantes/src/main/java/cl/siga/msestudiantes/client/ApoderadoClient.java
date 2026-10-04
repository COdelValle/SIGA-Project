package cl.siga.msestudiantes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;

@FeignClient(
    name = "ms-apoderados",
    url = "${services.ms-apoderados.url}",
    fallbackFactory = ApoderadoClientFallbackFactory.class
)
public interface ApoderadoClient {
    @GetMapping("/api/v1/apoderados/idUsuario/{idUsuario}")
    ApoderadoResponseDTO getApoderadoByIdUsuario(@PathVariable("idUsuario") String idUsuario);
}

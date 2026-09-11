package cl.siga.bffweb.integration.Notas;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.bffweb.config.FeignClientConfig;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;

@FeignClient (
    name = "ms-notas",
    url = "${microservices.notas.url}",
    configuration = FeignClientConfig.class,
    fallback = NotaClientFallback.class
)
public interface NotaClient {
    @GetMapping ("/api/v1/notas/search")
    List<NotaResponseDTO> getNotaByIdEstudiante(
        @RequestParam ("idEstudiante") Long idEstudiante
    );
}

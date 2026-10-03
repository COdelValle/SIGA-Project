package cl.siga.bffweb.integration.notas;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;

@FeignClient (
    name = "ms-notas",
    url = "${services.notas.url}",
    fallback = NotaClientFallback.class
)
public interface NotaClient {
    @GetMapping ("/api/v1/notas/search")
    PageResponseDTO<NotaResponseDTO> searchNotas(
        @RequestParam ("idEstudiante") Long idEstudiante,
        @RequestParam ("size") int size);
}

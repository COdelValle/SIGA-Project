package cl.siga.bffweb.integration.notas;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;

@FeignClient (
    name = "ms-notas",
    url = "${services.notas.url}",
    fallbackFactory = NotaClientFallbackFactory.class
)
public interface NotaClient {
    @GetMapping ("/api/v1/notas/{id}")
    NotaResponseDTO getNotaById(@PathVariable ("id") Long id);

    @GetMapping ("/api/v1/notas/search")
    PageResponseDTO<NotaResponseDTO> searchNotas(
        @RequestParam ("idEstudiante") Long idEstudiante,
        @RequestParam ("size") int size);

    @GetMapping ("/api/v1/notas/search")
    PageResponseDTO<NotaResponseDTO> searchNotasByEvaluacion(
        @RequestParam ("idEvaluacion") Long idEvaluacion,
        @RequestParam ("size") int size);

    @PostMapping ("/api/v1/notas")
    NotaResponseDTO saveNota(@RequestBody RegistrarNotaRequestDTO request);

    @PutMapping ("/api/v1/notas/{id}")
    NotaResponseDTO updateNota(
        @PathVariable ("id") Long id,
        @RequestBody ActualizarNotaRequestDTO request);

    @DeleteMapping ("/api/v1/notas/{id}")
    void deleteNota(@PathVariable ("id") Long id);
}

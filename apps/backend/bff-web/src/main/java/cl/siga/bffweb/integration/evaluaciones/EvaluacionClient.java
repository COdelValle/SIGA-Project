package cl.siga.bffweb.integration.evaluaciones;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;

@FeignClient (
    name = "ms-evaluaciones",
    url = "${services.evaluaciones.url}",
    fallbackFactory = EvaluacionClientFallbackFactory.class
)
public interface EvaluacionClient {
    @GetMapping ("/api/v1/evaluaciones/{id}")
    EvaluacionResponseDTO getEvaluacionById(@PathVariable ("id") Long id);

    @GetMapping ("/api/v1/evaluaciones/search")
    PageResponseDTO<EvaluacionResponseDTO> searchEvaluacionesByAsignatura(
        @RequestParam ("idAsignatura") Long idAsignatura,
        @RequestParam ("size") int size);

    @PostMapping ("/api/v1/evaluaciones")
    EvaluacionResponseDTO saveEvaluacion(@RequestBody RegistrarEvaluacionRequestDTO request);

    @PutMapping ("/api/v1/evaluaciones/{id}")
    EvaluacionResponseDTO updateEvaluacion(
        @PathVariable ("id") Long id,
        @RequestBody ActualizarEvaluacionRequestDTO request);

    @DeleteMapping ("/api/v1/evaluaciones/{id}")
    void deleteEvaluacion(@PathVariable ("id") Long id);
}

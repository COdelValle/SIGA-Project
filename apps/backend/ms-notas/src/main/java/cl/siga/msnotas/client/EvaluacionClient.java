package cl.siga.msnotas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;

@FeignClient(
    name = "ms-evaluaciones",
    url = "${services.ms-evaluaciones.url}",
    fallbackFactory = EvaluacionClientFallbackFactory.class
)
public interface EvaluacionClient {

    @GetMapping("/api/v1/evaluaciones/exists/{id}")
    boolean existsById(@PathVariable("id") Long id);

    /** Nombre e idCursoAsignatura de la evaluacion, para enriquecer la notificacion. */
    @GetMapping("/api/v1/evaluaciones/{id}")
    EvaluacionResponseDTO getEvaluacionById(@PathVariable("id") Long id);
}

package cl.siga.msnotas.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.exception.FeignFallbacks;

@Component
public class EvaluacionClientFallbackFactory implements FallbackFactory<EvaluacionClient> {

    @Override
    public EvaluacionClient create(Throwable cause) {
        return new EvaluacionClient() {
            @Override
            public boolean existsById(Long id) {
                throw FeignFallbacks.noDisponible(cause,
                    "No se pudo verificar la existencia de la evaluación " + id);
            }

            @Override
            public EvaluacionResponseDTO getEvaluacionById(Long id) {
                throw FeignFallbacks.noDisponible(cause,
                    "No se pudo obtener la evaluación " + id);
            }
        };
    }
}

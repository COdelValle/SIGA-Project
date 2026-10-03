package cl.siga.bffweb.integration.evaluaciones;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component
public class EvaluacionClientFallback implements EvaluacionClient {
    @Override
    public EvaluacionResponseDTO getEvaluacionById(Long id) {
        throw new ServiceUnavailableException("No se pudo obtener la evaluacion " + id);
    }

    @Override
    public PageResponseDTO<EvaluacionResponseDTO> searchEvaluacionesByAsignatura(Long idAsignatura, int size) {
        throw new ServiceUnavailableException("No se pudieron obtener las evaluaciones de la asignatura " + idAsignatura);
    }
}

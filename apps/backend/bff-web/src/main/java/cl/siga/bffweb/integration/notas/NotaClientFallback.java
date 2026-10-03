package cl.siga.bffweb.integration.notas;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component 
public class NotaClientFallback implements NotaClient {
    @Override 
    public PageResponseDTO<NotaResponseDTO> searchNotas(Long idEstudiante, int size) {
        throw new ServiceUnavailableException("No se pudo obtener las notas del estudiante " + idEstudiante);
    }
}

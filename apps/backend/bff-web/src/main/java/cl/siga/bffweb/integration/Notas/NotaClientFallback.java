package cl.siga.bffweb.integration.Notas;

import java.util.List;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component 
public class NotaClientFallback implements NotaClient {
    @Override 
    public List<NotaResponseDTO> getNotaByIdEstudiante(Long idEstudiante) {
        throw new ServiceUnavailableException("No se pudo obtener las notas del estudiante " + idEstudiante);
    }
}

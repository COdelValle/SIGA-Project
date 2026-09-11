package cl.siga.bffweb.integration.asignaturas;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component 
public class AsignaturaClientFallback implements AsignaturaClient {

    @Override
    public AsignaturaResponseDTO getAsignaturaById(Long id) {
        throw new ServiceUnavailableException("No se pudo obtener la asignatura " + id);
    }
}


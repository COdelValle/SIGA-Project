package cl.siga.bffweb.integration.estudiantes;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component 
public class EstudianteClientFallback implements EstudianteClient{
    @Override
    public EstudianteResponseDTO getEstudianteById(String id) {
        throw new ServiceUnavailableException("No se pudo obtener el estudiante " + id);
    }
}

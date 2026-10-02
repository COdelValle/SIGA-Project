package cl.siga.msasignaturas.client;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component 
public class EstudianteClientFallback implements EstudianteClient {
    @Override 
    public EstudianteResponseDTO getEstudianteByIdUsuario(String idUsuario) {
        throw new ServiceUnavailableException("No se pudo verificar el estudiante con ID de usuario " + idUsuario);
    }
}

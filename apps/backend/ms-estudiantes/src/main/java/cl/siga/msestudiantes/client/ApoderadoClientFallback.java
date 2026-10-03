package cl.siga.msestudiantes.client;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component 
public class ApoderadoClientFallback implements ApoderadoClient {
    @Override 
    public ApoderadoResponseDTO getApoderadoByIdUsuario(String idUsuario) {
        throw new ServiceUnavailableException("No se pudo verificar el apoderado con ID de usuario " + idUsuario);
    }
}

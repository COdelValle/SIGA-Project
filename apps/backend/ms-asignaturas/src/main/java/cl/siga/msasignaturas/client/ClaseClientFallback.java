package cl.siga.msasignaturas.client;

import cl.siga.coreshare.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;

@Component 
public class ClaseClientFallback implements ClaseClient {
    @Override
    public boolean existsById(Long id) {
        throw new ServiceUnavailableException("No se pudo verificar la existencia de la clase " + id);
    }
}

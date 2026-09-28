package cl.siga.msclases.client;

import cl.siga.coreshare.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class DocenteClientFallback implements DocenteClient {

    @Override
    public boolean existsById(Long id) {
        throw new ServiceUnavailableException("No se pudo verificar la existencia del docente " + id);
    }
}

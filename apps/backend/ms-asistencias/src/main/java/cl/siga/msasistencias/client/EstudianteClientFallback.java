package cl.siga.msasistencias.client;

import cl.siga.coreshare.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class EstudianteClientFallback implements EstudianteClient {

    @Override
    public boolean existsById(Long id) {
        throw new ServiceUnavailableException("No se pudo verificar la existencia del estudiante " + id);
    }
}

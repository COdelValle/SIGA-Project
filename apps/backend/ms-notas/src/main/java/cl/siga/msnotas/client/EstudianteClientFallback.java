package cl.siga.msnotas.client;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component
public class EstudianteClientFallback implements EstudianteClient {

    @Override
    public boolean existsById(Long id) {
        throw new ServiceUnavailableException("No se pudo verificar la existencia del estudiante " + id);
    }
}

package cl.siga.msnotas.client;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component
public class AsignaturaClientFallback implements AsignaturaClient {

    @Override
    public boolean existsById(Long id) {
        throw new ServiceUnavailableException("No se pudo verificar la existencia de la asignatura " + id);
    }
}

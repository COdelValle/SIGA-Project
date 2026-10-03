package cl.siga.msasistencias.client;

import cl.siga.coreshare.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class AsignaturaClientFallback implements AsignaturaClient {

    @Override
    public boolean existsById(Long id) {
        throw new ServiceUnavailableException("No se pudo verificar la existencia de la asignatura " + id);
    }
}

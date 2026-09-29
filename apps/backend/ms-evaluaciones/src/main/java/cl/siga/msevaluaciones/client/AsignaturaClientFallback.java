package cl.siga.msevaluaciones.client;

import cl.siga.coreshare.exception.ServiceUnavailableException;
import cl.siga.msnotas.client.AsignaturaClient;
import org.springframework.stereotype.Component;

@Component
public class AsignaturaClientFallback implements AsignaturaClient {

    @Override
    public boolean existsById(Long id) {
        throw new ServiceUnavailableException("No se pudo verificar la existencia de la asignatura " + id);
    }
}

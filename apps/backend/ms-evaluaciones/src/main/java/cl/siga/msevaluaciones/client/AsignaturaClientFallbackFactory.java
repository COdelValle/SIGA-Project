package cl.siga.msevaluaciones.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.exception.FeignFallbacks;

@Component
public class AsignaturaClientFallbackFactory implements FallbackFactory<AsignaturaClient> {

    @Override
    public AsignaturaClient create(Throwable cause) {
        return new AsignaturaClient() {
            @Override
            public boolean existsById(Long id) {
                throw FeignFallbacks.noDisponible(cause,
                    "No se pudo verificar la existencia de la asignatura " + id);
            }
        };
    }
}

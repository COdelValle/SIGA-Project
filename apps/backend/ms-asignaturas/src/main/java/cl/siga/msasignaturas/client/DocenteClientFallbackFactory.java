package cl.siga.msasignaturas.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.exception.FeignFallbacks;

@Component
public class DocenteClientFallbackFactory implements FallbackFactory<DocenteClient> {

    @Override
    public DocenteClient create(Throwable cause) {
        return new DocenteClient() {
            @Override
            public boolean existsById(Long id) {
                throw FeignFallbacks.noDisponible(cause,
                    "No se pudo verificar la existencia del docente " + id);
            }
        };
    }
}

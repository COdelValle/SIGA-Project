package cl.siga.msapoderados.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.exception.FeignFallbacks;

@Component
public class EstudianteClientFallbackFactory implements FallbackFactory<EstudianteClient> {

    @Override
    public EstudianteClient create(Throwable cause) {
        return new EstudianteClient() {
            @Override
            public boolean existsById(Long id) {
                throw FeignFallbacks.noDisponible(cause,
                    "No se pudo verificar la existencia del estudiante " + id);
            }
        };
    }
}

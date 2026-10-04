package cl.siga.msasignaturas.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.exception.FeignFallbacks;

@Component
public class ClaseClientFallbackFactory implements FallbackFactory<ClaseClient> {

    @Override
    public ClaseClient create(Throwable cause) {
        return new ClaseClient() {
            @Override
            public boolean existsById(Long id) {
                throw FeignFallbacks.noDisponible(cause,
                    "No se pudo verificar la existencia de la clase " + id);
            }

            @Override
            public ClaseResponseDTO getClaseById(Long id) {
                throw FeignFallbacks.noDisponible(cause,
                    "No se pudo obtener la clase " + id);
            }
        };
    }
}

package cl.siga.msnotas.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.exception.FeignFallbacks;

@Component
public class AsignaturaClientFallbackFactory implements FallbackFactory<AsignaturaClient> {

    @Override
    public AsignaturaClient create(Throwable cause) {
        return new AsignaturaClient() {
            @Override
            public CursoAsignaturaResponseDTO getCursoAsignaturaById(Long id) {
                throw FeignFallbacks.noDisponible(cause,
                    "No se pudo obtener la dictación " + id);
            }
        };
    }
}

package cl.siga.msestudiantes.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.exception.FeignFallbacks;

@Component
public class ApoderadoClientFallbackFactory implements FallbackFactory<ApoderadoClient> {

    @Override
    public ApoderadoClient create(Throwable cause) {
        return new ApoderadoClient() {
            @Override
            public ApoderadoResponseDTO getApoderadoByIdUsuario(String idUsuario) {
                throw FeignFallbacks.noDisponible(cause,
                    "No se pudo verificar el apoderado con ID de usuario " + idUsuario);
            }
        };
    }
}

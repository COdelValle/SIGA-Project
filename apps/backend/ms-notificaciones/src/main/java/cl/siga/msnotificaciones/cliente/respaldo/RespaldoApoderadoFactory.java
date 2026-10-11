package cl.siga.msnotificaciones.cliente.respaldo;

import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.exception.FeignFallbacks;
import cl.siga.msnotificaciones.cliente.ClienteApoderado;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Si ms-apoderados no responde, avisa con un error claro. */
@Component
public class RespaldoApoderadoFactory implements FallbackFactory<ClienteApoderado> {

    @Override
    public ClienteApoderado create(Throwable causa) {
        return new ClienteApoderado() {
            @Override
            public ApoderadoResponseDTO obtenerPorIdUsuario(String idUsuario) {
                throw FeignFallbacks.noDisponible(causa,
                    "No se pudo obtener el apoderado de la cuenta autenticada");
            }

            @Override
            public PageResponseDTO<ApoderadoResponseDTO> buscarPorEstudiante(Long idEstudiante, int tamano) {
                throw FeignFallbacks.noDisponible(causa,
                    "No se pudieron obtener los apoderados del estudiante " + idEstudiante);
            }
        };
    }
}

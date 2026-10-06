package cl.siga.msnotificaciones.cliente.respaldo;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.exception.FeignFallbacks;
import cl.siga.msnotificaciones.cliente.ClienteAsignatura;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Si ms-asignaturas no responde, avisa con un error claro en vez de fallar a ciegas. */
@Component
public class RespaldoAsignaturaFactory implements FallbackFactory<ClienteAsignatura> {

    @Override
    public ClienteAsignatura create(Throwable causa) {
        return new ClienteAsignatura() {
            @Override
            public CursoAsignaturaResponseDTO obtenerDictacion(Long id) {
                throw FeignFallbacks.noDisponible(causa,
                    "No se pudo obtener la dictación " + id);
            }
        };
    }
}

package cl.siga.msnotificaciones.cliente.respaldo;

import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.exception.FeignFallbacks;
import cl.siga.msnotificaciones.cliente.ClienteInscripcion;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Si ms-asignaturas no responde al pedir inscritos, avisa con error claro. */
@Component
public class RespaldoInscripcionFactory implements FallbackFactory<ClienteInscripcion> {

    @Override
    public ClienteInscripcion create(Throwable causa) {
        return new ClienteInscripcion() {
            @Override
            public PageResponseDTO<InscripcionResponseDTO> buscarPorDictacion(Long idCursoAsignatura, int tamano) {
                throw FeignFallbacks.noDisponible(causa,
                    "No se pudieron obtener las inscripciones de la dictación " + idCursoAsignatura);
            }
        };
    }
}

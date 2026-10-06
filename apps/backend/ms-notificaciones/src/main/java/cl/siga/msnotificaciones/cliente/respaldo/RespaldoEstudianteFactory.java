package cl.siga.msnotificaciones.cliente.respaldo;

import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.exception.FeignFallbacks;
import cl.siga.msnotificaciones.cliente.ClienteEstudiante;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Si ms-estudiantes no responde, avisa con un error claro. */
@Component
public class RespaldoEstudianteFactory implements FallbackFactory<ClienteEstudiante> {

    @Override
    public ClienteEstudiante create(Throwable causa) {
        return new ClienteEstudiante() {
            @Override
            public EstudianteResponseDTO obtenerPorId(Long id) {
                throw FeignFallbacks.noDisponible(causa,
                    "No se pudo obtener el estudiante " + id);
            }
        };
    }
}

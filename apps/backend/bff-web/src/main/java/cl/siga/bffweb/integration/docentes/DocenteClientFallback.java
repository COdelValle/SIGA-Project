package cl.siga.bffweb.integration.docentes;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component
public class DocenteClientFallback implements DocenteClient {
    @Override
    public DocenteResponseDTO getDocenteById(Long id) {
        throw new ServiceUnavailableException("No se pudo obtener el docente " + id);
    }

    @Override
    public DocenteResponseDTO getDocenteByIdUsuario(String idUsuario) {
        throw new ServiceUnavailableException("No se pudo obtener el docente con idUsuario " + idUsuario);
    }
}

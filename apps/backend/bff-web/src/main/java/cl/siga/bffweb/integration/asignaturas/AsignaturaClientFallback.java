package cl.siga.bffweb.integration.asignaturas;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component
public class AsignaturaClientFallback implements AsignaturaClient {
    @Override
    public AsignaturaResponseDTO getAsignaturaById(Long id) {
        throw new ServiceUnavailableException("No se pudo obtener la asignatura " + id);
    }

    @Override
    public PageResponseDTO<AsignaturaResponseDTO> searchAsignaturasByClase(Long idClase, int size) {
        throw new ServiceUnavailableException("No se pudieron obtener las asignaturas de la clase " + idClase);
    }

    @Override
    public PageResponseDTO<AsignaturaResponseDTO> searchAsignaturasByDocente(Long idDocente, int size) {
        throw new ServiceUnavailableException("No se pudieron obtener las asignaturas del docente " + idDocente);
    }

    @Override
    public PageResponseDTO<AsignaturaResponseDTO> searchAsignaturas(int size) {
        throw new ServiceUnavailableException("No se pudieron obtener las asignaturas");
    }
}

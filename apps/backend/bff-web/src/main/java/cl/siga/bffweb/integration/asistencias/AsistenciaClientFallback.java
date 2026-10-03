package cl.siga.bffweb.integration.asistencias;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component
public class AsistenciaClientFallback implements AsistenciaClient {
    @Override
    public PageResponseDTO<AsistenciaResponseDTO> searchAsistencias(Long idEstudiante, int size) {
        throw new ServiceUnavailableException("No se pudieron obtener las asistencias del estudiante " + idEstudiante);
    }

    @Override
    public AsistenciaResponseDTO saveAsistencia(RegistrarAsistenciaRequestDTO request) {
        throw new ServiceUnavailableException("No se pudo registrar la asistencia");
    }

    @Override
    public AsistenciaResponseDTO updateAsistencia(Long id, ActualizarAsistenciaRequestDTO request) {
        throw new ServiceUnavailableException("No se pudo actualizar la asistencia " + id);
    }
}

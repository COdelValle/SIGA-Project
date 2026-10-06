package cl.siga.bffweb.integration.estudiantes;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component 
public class EstudianteClientFallback implements EstudianteClient{
    @Override
    public EstudianteResponseDTO getEstudianteById(Long id) {
        throw new ServiceUnavailableException("No se pudo obtener el estudiante " + id);
    }

    @Override
    public EstudianteResponseDTO getEstudianteByIdUsuario(String idUsuario) {
        throw new ServiceUnavailableException("No se pudo obtener el estudiante con idUsuario " + idUsuario);
    }

    @Override
    public PageResponseDTO<EstudianteResponseDTO> searchEstudiantesByClase(Long idClase, int size) {
        throw new ServiceUnavailableException("No se pudieron obtener los estudiantes de la clase " + idClase);
    }

    @Override
    public PageResponseDTO<EstudianteResponseDTO> searchEstudiantes(String q, int size) {
        throw new ServiceUnavailableException("No se pudieron buscar estudiantes para '" + q + "'");
    }

    @Override
    public EstudianteResponseDTO updateEstudiante(Long id, ActualizarEstudianteRequestDTO request) {
        throw new ServiceUnavailableException("No se pudo actualizar el estudiante " + id);
    }
}

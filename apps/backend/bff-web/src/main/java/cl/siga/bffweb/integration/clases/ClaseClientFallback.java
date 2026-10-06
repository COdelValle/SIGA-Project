package cl.siga.bffweb.integration.clases;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component
public class ClaseClientFallback implements ClaseClient {
    @Override
    public ClaseResponseDTO getClaseById(Long id) {
        throw new ServiceUnavailableException("No se pudo obtener la clase " + id);
    }

    @Override
    public PageResponseDTO<ClaseResponseDTO> searchClases(Integer anioAcademico, int size) {
        throw new ServiceUnavailableException(
            "No se pudieron obtener las clases del año " + anioAcademico);
    }
}

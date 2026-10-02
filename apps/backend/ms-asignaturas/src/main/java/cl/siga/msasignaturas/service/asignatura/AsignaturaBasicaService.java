package cl.siga.msasignaturas.service.asignatura;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.basica.ActualizarAsignaturaBasicaRequestDTO;
import cl.siga.coreshare.dto.asignatura.basica.RegistrarAsignaturaBasicaRequestDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.client.ClaseClient;
import cl.siga.msasignaturas.client.DocenteClient;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaBasica;
import cl.siga.msasignaturas.model.mapper.AsignaturaMapper;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaBasicaRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AsignaturaBasicaService {

    private final AsignaturaBasicaRepository repository;
    private final AsignaturaMapper mapper;
    private final DocenteClient docenteClient;
    private final ClaseClient claseClient;

    @Transactional 
    public AsignaturaResponseDTO crearBasica(RegistrarAsignaturaBasicaRequestDTO dto) {
        // Validación de existencia de Clase
        if (dto.idClase() != null && !claseClient.existsById(dto.idClase())) {
            throw new BusinessException("La clase con ID " + dto.idClase() + " no existe o no está activa.");
        }

        // Validación de existencia de Docente
        if (dto.idDocente() != null && !docenteClient.existsById(dto.idDocente())) {
            throw new BusinessException("El docente con ID " + dto.idDocente() + " no existe o no está activo.");
        }

        // Regla de Negocio: Evitar duplicidad exacta en la misma clase
        if (repository.existsByNameIgnoreCaseAndSemestreAndIdClaseAndActiveTrue(
                dto.name(), dto.semestre(), dto.idClase())) {
            throw new BusinessException("Ya existe una asignatura básica con ese nombre y semestre para esta clase.");
        }

        AsignaturaBasica entidad = mapper.toEntity(dto);
        return mapper.toResponseDto(repository.save(entidad));
    }

    @Transactional
    public AsignaturaResponseDTO actualizarBasica(Long id, ActualizarAsignaturaBasicaRequestDTO dto) {
        AsignaturaBasica entidad = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura Básica con ID " + id + " no encontrada."));

        // Validación si se actualiza el Docente
        if (dto.idDocente() != null && !dto.idDocente().equals(entidad.getIdDocente())) {
            if (!docenteClient.existsById(dto.idDocente())) {
                throw new BusinessException("El docente con ID " + dto.idDocente() + " no existe o no está activo.");
            }
        }

        mapper.updateBasicaFromDto(dto, entidad);
        return mapper.toResponseDto(repository.save(entidad));
    }
}
package cl.siga.msasignaturas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import cl.siga.coreshare.dto.asignatura.AsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.model.entity.Asignatura;
import cl.siga.msasignaturas.model.mapper.AsignaturaMapper;
import cl.siga.msasignaturas.repository.AsignaturaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Validated 
@RequiredArgsConstructor
public class AsignaturaService {
    private final AsignaturaRepository repository;

    private final AsignaturaMapper mapper;

    @Transactional (readOnly = true)
    public AsignaturaResponseDTO getAsignaturaById(Long id) {
        return mapper.toResponseDto(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura con ID " + id + " no encontrada.")));
    }

    @Transactional (readOnly = true)
    public AsignaturaResponseDTO getAsignaturaByName(String name) {
        return mapper.toResponseDto(repository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura con nombre " + name + " no encontrada.")));
    }

    @Transactional 
    public AsignaturaResponseDTO saveAsignatura(@Valid AsignaturaRequestDTO request) {
        if(repository.existsByName(request.name())) {
            throw new BusinessException("Ya existe una asignatura con el nombre: " + request.name());
        }
        return mapper.toResponseDto(repository.save(mapper.toEntity(request)));
    }

    @Transactional
    public AsignaturaResponseDTO updateAsignatura(Long id, @Valid AsignaturaRequestDTO request) {
        Asignatura existingAsignatura = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura con ID " + id + " no encontrada."));
        
        if(!existingAsignatura.getName().equals(request.name()) && repository.existsByName(request.name())) {
            throw new BusinessException("Ya existe una asignatura con el nombre: " + request.name());
        }

        mapper.updateEntityFromDto(request, existingAsignatura);

        return mapper.toResponseDto(repository.save(existingAsignatura));
    }

    @Transactional (readOnly = true)
    public boolean existsAsignaturaById(Long id) {
        return repository.existsById(id);
    }
}

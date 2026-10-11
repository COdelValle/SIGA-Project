package cl.siga.msclases.service;

import cl.siga.coreshare.dto.clase.ActualizarDocenteJefeRequestDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.RegistrarClaseRequestDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msclases.client.DocenteClient;
import cl.siga.msclases.model.entity.Clase;
import cl.siga.msclases.model.mapper.ClaseMapper;
import cl.siga.msclases.model.specifications.ClaseSpecifications;
import cl.siga.msclases.repository.ClaseRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
public class ClaseService {
  private final ClaseRepository repository;
  private final ClaseMapper mapper;
  private final DocenteClient docenteClient;

  @Transactional(readOnly = true)
  public ClaseResponseDTO getClaseById(Long id) {
    return mapper.toResponseDto(repository.findByIdAndActiveTrue(id)
      .orElseThrow(() -> new ResourceNotFoundException("Clase con ID " + id + " no encontrada.")));
  }

  @Transactional(readOnly = true)
  public Page<ClaseResponseDTO> searchClases(Nivel nivel, String letra, Integer anioAcademico, Long idDocenteJefe, Pageable pageable) {
    Specification<Clase> spec = ClaseSpecifications.isActive()
      .and(ClaseSpecifications.hasNivel(nivel))
      .and(ClaseSpecifications.hasLetra(letra))
      .and(ClaseSpecifications.hasAnioAcademico(anioAcademico))
      .and(ClaseSpecifications.hasIdDocenteJefe(idDocenteJefe));

    return repository.findAll(spec, pageable).map(mapper::toResponseDto);
  }

  @Transactional
  public ClaseResponseDTO saveClase(@Valid RegistrarClaseRequestDTO request) {
    String letraNormalizada = request.letra() != null ? request.letra().trim().toUpperCase() : null;
    Optional<Clase> existente = repository.findByNivelAndLetraAndAnioAcademico(
      request.nivel(), letraNormalizada, request.anioAcademico());

    if (existente.isPresent() && existente.get().isActive()) {
      throw new BusinessException(
        String.format("Ya existe la clase %s %s para el año académico %d.",
          request.nivel(), letraNormalizada, request.anioAcademico())
      );
    }

    if (request.idDocenteJefe() != null) {
      if (!docenteClient.existsById(request.idDocenteJefe())) {
        throw new BusinessException("El docente con ID " + request.idDocenteJefe() + " no existe o no está activo.");
      }

      if (repository.existsByIdDocenteJefeAndActiveTrue(request.idDocenteJefe())) {
        throw new BusinessException("El docente ya es jefe de otra clase activa.");
      }
    }

    // Si la clase fue eliminada logicamente, se reactiva en vez de chocar con el UNIQUE.
    if (existente.isPresent()) {
      Clase clase = existente.get();
      clase.setIdDocenteJefe(request.idDocenteJefe());
      clase.setActive(true);
      return mapper.toResponseDto(repository.save(clase));
    }

    return mapper.toResponseDto(repository.save(mapper.toEntity(request)));
  }

  @Transactional
  public ClaseResponseDTO updateDocenteJefe(Long idClase, @Valid ActualizarDocenteJefeRequestDTO request) {
    Clase existingClase = repository.findByIdAndActiveTrue(idClase)
      .orElseThrow(() -> new ResourceNotFoundException("Clase con ID " + idClase + " no encontrada."));

    if (request.idDocenteJefe() != null) {
      if (!docenteClient.existsById(request.idDocenteJefe())) {
        throw new BusinessException("El docente con ID " + request.idDocenteJefe() + " no existe o no está activo.");
      }

      if (repository.existsByIdDocenteJefeAndActiveTrueAndIdNot(request.idDocenteJefe(), idClase)) {
        throw new BusinessException("El docente ya es jefe de otra clase activa.");
      }
    }

    // Se asigna explicitamente para permitir limpiar el docente jefe (null).
    existingClase.setIdDocenteJefe(request.idDocenteJefe());
    return mapper.toResponseDto(repository.save(existingClase));
  }

  @Transactional(readOnly = true)
  public boolean existsClaseById(Long id) {
    return repository.existsByIdAndActiveTrue(id);
  }

  @Transactional
  public void deleteClase(Long id) {
    Clase existingClase = repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Clase con ID " + id + " no encontrada."));
    existingClase.setActive(false);
    repository.save(existingClase);
  }
}

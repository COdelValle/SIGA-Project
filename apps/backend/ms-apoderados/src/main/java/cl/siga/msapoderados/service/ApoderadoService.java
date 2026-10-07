package cl.siga.msapoderados.service;

import cl.siga.coreshare.dto.apoderado.ActualizarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.RegistrarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.format.RutNormalizer;
import cl.siga.coreshare.security.SecurityUtils;
import cl.siga.msapoderados.client.EstudianteClient;
import cl.siga.msapoderados.model.entity.Apoderado;
import cl.siga.msapoderados.model.entity.ApoderadoEstudiante;
import cl.siga.msapoderados.model.mapper.ApoderadoMapper;
import cl.siga.msapoderados.model.specifications.ApoderadoSpecifications;
import cl.siga.msapoderados.repository.ApoderadoRepository;
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
public class ApoderadoService {

  private final ApoderadoRepository repository;
  private final ApoderadoMapper mapper;
  private final EstudianteClient estudianteClient;

  @Transactional(readOnly = true)
  public ApoderadoResponseDTO getApoderadoById(Long id) {
    return mapper.toResponseDto(repository.findByIdAndActivoTrue(id)
      .orElseThrow(() -> new ResourceNotFoundException("Apoderado con ID " + id + " no encontrado.")));
  }

  @Transactional(readOnly = true)
  public ApoderadoResponseDTO getApoderadoByIdUsuario(String idUsuario) {
    return mapper.toResponseDto(repository.findByIdUsuarioAndActivoTrue(idUsuario)
      .orElseThrow(() -> new ResourceNotFoundException("Apoderado con ID de usuario " + idUsuario + " no encontrado.")));
  }

  @Transactional(readOnly = true)
  public Page<ApoderadoResponseDTO> searchApoderados(String rut, String firstName, String firstSurname, Long idEstudiante, Pageable pageable) {
    Specification<Apoderado> spec = ApoderadoSpecifications.isActivo()
      .and(ApoderadoSpecifications.hasRut(rut))
      .and(ApoderadoSpecifications.hasFirstName(firstName))
      .and(ApoderadoSpecifications.hasFirstSurname(firstSurname))
      .and(ApoderadoSpecifications.hasIdEstudiante(idEstudiante));

    return repository.findAll(spec, pageable).map(mapper::toResponseDto);
  }

  @Transactional
  public ApoderadoResponseDTO saveApoderado(@Valid RegistrarApoderadoRequestDTO request) {
    return guardar(request, true);
  }

  /**
   * Alta usada por el consumidor del registro asíncrono: los pupilos ya fueron
   * validados de forma síncrona al aceptar la solicitud, así que no se repite
   * la llamada Feign (el listener no tiene JWT de usuario).
   */
  @Transactional
  public ApoderadoResponseDTO saveApoderadoDesdeEvento(@Valid RegistrarApoderadoRequestDTO request) {
    return guardar(request, false);
  }

  private ApoderadoResponseDTO guardar(RegistrarApoderadoRequestDTO request, boolean validarEstudiantes) {
    String normalizedRut = RutNormalizer.normalizar(request.rut());

    // Apoderado que vuelve: si el perfil existe inactivo, se reactiva con los datos recibidos.
    Optional<Apoderado> existente = repository.findByIdUsuario(request.idUsuario());
    if (existente.isPresent()) {
      Apoderado apoderado = existente.get();
      if (Boolean.TRUE.equals(apoderado.getActivo())) {
        throw new BusinessException("El ID de usuario de Azure ya está registrado: " + request.idUsuario());
      }
      reactivarPerfil(apoderado, request, normalizedRut, validarEstudiantes);
      return mapper.toResponseDto(repository.save(apoderado));
    }

    if (repository.existsByRut(normalizedRut)) {
      throw new BusinessException("El RUT ya está registrado: " + request.rut());
    }

    if (validarEstudiantes) {
      validarEstudiantes(request);
    }

    Apoderado apoderado = mapper.toEntity(request);
    apoderado.setActivo(true);

    return mapper.toResponseDto(repository.save(apoderado));
  }

  private void reactivarPerfil(Apoderado apoderado, RegistrarApoderadoRequestDTO request,
      String normalizedRut, boolean validarEstudiantes) {
    if (normalizedRut != null && !normalizedRut.equalsIgnoreCase(apoderado.getRut())
      && repository.existsByRut(normalizedRut)) {
      throw new BusinessException("El RUT ya está registrado: " + request.rut());
    }
    if (validarEstudiantes) {
      validarEstudiantes(request);
    }
    if (normalizedRut != null) {
      apoderado.setRut(normalizedRut);
    }
    apoderado.setFirstName(request.firstName());
    apoderado.setMiddleName(request.middleName());
    apoderado.setFirstSurname(request.firstSurname());
    apoderado.setSecondSurname(request.secondSurname());
    apoderado.getTelefonos().clear();
    apoderado.getTelefonos().addAll(request.telefonos());
    apoderado.getEstudiantes().clear();
    request.estudiantes().forEach(vinculo ->
      apoderado.getEstudiantes().add(mapper.toApoderadoEstudianteEntity(vinculo)));
    apoderado.setActivo(true);
  }

  private void validarEstudiantes(RegistrarApoderadoRequestDTO request) {
    for (ParentescoEstudianteDTO estudianteRelacion : request.estudiantes()) {
      if (!estudianteClient.existsById(estudianteRelacion.idEstudiante())) {
        throw new BusinessException("El estudiante con ID " + estudianteRelacion.idEstudiante() + " no existe.");
      }
    }
  }

  @Transactional
  public ApoderadoResponseDTO updateApoderado(Long id, @Valid ActualizarApoderadoRequestDTO request) {
    Apoderado apoderadoExistente = repository.findByIdAndActivoTrue(id)
      .orElseThrow(() -> new ResourceNotFoundException("Apoderado con ID " + id + " no encontrado."));

    SecurityUtils.requireOwnerOrAdmin(
      apoderadoExistente.getIdUsuario(),
      "No tienes permiso para modificar este apoderado.");

    mapper.updateEntityFromDto(request, apoderadoExistente);
    return mapper.toResponseDto(repository.save(apoderadoExistente));
  }

  // --- MÉTODOS PARA GESTIONAR ESTUDIANTES ---
  @Transactional
  public ApoderadoResponseDTO addEstudiante(Long idApoderado, @Valid ParentescoEstudianteDTO request) {
    Apoderado apoderado = repository.findByIdAndActivoTrue(idApoderado)
      .orElseThrow(() -> new ResourceNotFoundException("Apoderado con ID " + idApoderado + " no encontrado."));

    // 1. Verificar que el estudiante no esté ya vinculado a este apoderado
    boolean yaVinculado = apoderado.getEstudiantes().stream()
      .anyMatch(e -> e.getIdEstudiante().equals(request.idEstudiante()));

    if (yaVinculado) {
      throw new BusinessException("El estudiante con ID " + request.idEstudiante() + " ya está vinculado a este apoderado.");
    }

    // 2. Validar existencia real del estudiante en ms-estudiantes mediante Feign
    if (!estudianteClient.existsById(request.idEstudiante())) {
      throw new BusinessException("El estudiante con ID " + request.idEstudiante() + " no existe.");
    }

    // 3. Convertir el DTO al embebible y agregarlo a la colección
    ApoderadoEstudiante nuevoVinculo = mapper.toApoderadoEstudianteEntity(request);
    apoderado.getEstudiantes().add(nuevoVinculo);

    return mapper.toResponseDto(repository.save(apoderado));
  }

  @Transactional
  public ApoderadoResponseDTO removeEstudiante(Long idApoderado, Long idEstudiante) {
    Apoderado apoderado = repository.findByIdAndActivoTrue(idApoderado)
      .orElseThrow(() -> new ResourceNotFoundException("Apoderado con ID " + idApoderado + " no encontrado."));

    // 1. Validar que el apoderado no se quede con 0 estudiantes
    boolean esElUnicoEstudiante = apoderado.getEstudiantes().size() == 1;
    boolean esElEstudianteAEliminar = apoderado.getEstudiantes().stream()
      .anyMatch(e -> e.getIdEstudiante().equals(idEstudiante));

    if (esElUnicoEstudiante && esElEstudianteAEliminar) {
      throw new BusinessException("No se puede eliminar el vínculo. El apoderado debe tener al menos un estudiante asignado.");
    }

    // 2. Se intenta remover el estudiante de la colección.
    boolean removido = apoderado.getEstudiantes().removeIf(e -> e.getIdEstudiante().equals(idEstudiante));

    if (!removido) {
      throw new ResourceNotFoundException("El estudiante con ID " + idEstudiante + " no está vinculado a este apoderado.");
    }

    // 3. Persistir los cambios
    return mapper.toResponseDto(repository.save(apoderado));
  }

  // -------------------------------------------------

  @Transactional(readOnly = true)
  public boolean existsApoderadoById(Long id) {
    return repository.existsByIdAndActivoTrue(id);
  }

  @Transactional
  public void deleteApoderado(Long id) {
    Apoderado existing = repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Apoderado con ID " + id + " no encontrado."));
    existing.setActivo(false);
    repository.save(existing);
  }
}

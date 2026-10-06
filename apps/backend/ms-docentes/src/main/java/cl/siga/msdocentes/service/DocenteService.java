package cl.siga.msdocentes.service;

import cl.siga.coreshare.dto.docente.ActualizarDocenteRequestDTO;
import cl.siga.coreshare.dto.docente.RegistrarDocenteRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.format.RutNormalizer;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.security.SecurityUtils;
import cl.siga.msdocentes.model.entity.Docente;
import cl.siga.msdocentes.model.mapper.CertificadoMapper;
import cl.siga.msdocentes.model.mapper.DocenteMapper;
import cl.siga.msdocentes.model.specifications.DocenteSpecifications;
import cl.siga.msdocentes.repository.DocenteRepository;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
public class DocenteService {
  private final DocenteRepository repository;
  private final DocenteMapper mapper;
  private final CertificadoMapper certificadoMapper;

  @Transactional(readOnly = true)
  public DocenteResponseDTO getDocenteById(Long id) {
    return mapper.toResponseDto(repository.findByIdAndActivoTrue(id)
      .orElseThrow(() -> new ResourceNotFoundException("Docente con ID " + id + " no encontrado.")));
  }

  @Transactional(readOnly = true)
  public DocenteResponseDTO getDocenteByIdUsuario(String idUsuario) {
    return mapper.toResponseDto(repository.findByIdUsuarioAndActivoTrue(idUsuario)
      .orElseThrow(() -> new ResourceNotFoundException("Docente con ID de usuario " + idUsuario + " no encontrado.")));
  }

  @Transactional(readOnly = true)
  public Page<DocenteResponseDTO> searchDocentes(String rut, String firstName, String firstSurname, LocalDate from, LocalDate to, AreaAcademica area, Pageable pageable) {
    Specification<Docente> spec = DocenteSpecifications.isActivo()
      .and(DocenteSpecifications.hasRut(rut))
      .and(DocenteSpecifications.hasFirstName(firstName))
      .and(DocenteSpecifications.hasFirstSurname(firstSurname))
      .and(DocenteSpecifications.hasFechaContratacionGreaterThanOrEqual(from))
      .and(DocenteSpecifications.hasFechaContratacionLessThanOrEqual(to))
      .and(DocenteSpecifications.hasArea(area));
    return repository.findAll(spec, pageable).map(mapper::toResponseDto);
  }

  @Transactional
  public DocenteResponseDTO saveDocente(@Valid RegistrarDocenteRequestDTO request) {
    String normalizedRut = RutNormalizer.normalizar(request.rut());

    // Docente que vuelve: si el perfil existe inactivo, se reactiva con los datos recibidos.
    Optional<Docente> existente = repository.findByIdUsuario(request.idUsuario());
    if (existente.isPresent()) {
      Docente docente = existente.get();
      if (Boolean.TRUE.equals(docente.getActivo())) {
        throw new BusinessException("El ID de usuario ya está registrado: " + request.idUsuario());
      }
      reactivarPerfil(docente, request, normalizedRut);
      return mapper.toResponseDto(repository.save(docente));
    }

    if (repository.existsByRut(normalizedRut)) {
      throw new BusinessException("El RUT ya está registrado: " + request.rut());
    }

    Docente docente = mapper.toEntity(request);
    docente.setActivo(true);

    return mapper.toResponseDto(repository.save(docente));
  }

  private void reactivarPerfil(Docente docente, RegistrarDocenteRequestDTO request, String normalizedRut) {
    if (normalizedRut != null && !normalizedRut.equalsIgnoreCase(docente.getRut())
      && repository.existsByRut(normalizedRut)) {
      throw new BusinessException("El RUT ya está registrado: " + request.rut());
    }
    if (normalizedRut != null) {
      docente.setRut(normalizedRut);
    }
    docente.setFirstName(request.firstName());
    docente.setMiddleName(request.middleName());
    docente.setFirstSurname(request.firstSurname());
    docente.setSecondSurname(request.secondSurname());
    docente.setFechaContratacion(request.fechaContratacion());
    docente.setArea(request.area());
    docente.getCertificados().clear();
    if (request.certificados() != null) {
      request.certificados().forEach(certificado ->
        docente.addCertificado(certificadoMapper.toEntity(certificado)));
    }
    docente.setActivo(true);
  }

  @Transactional
  public DocenteResponseDTO updateDocente(Long id, @Valid ActualizarDocenteRequestDTO request) {
    Docente docenteExistente = repository.findByIdAndActivoTrue(id)
      .orElseThrow(() -> new ResourceNotFoundException("Docente con ID " + id + " no encontrado."));

    SecurityUtils.requireOwnerOrAdmin(
      docenteExistente.getIdUsuario(),
      "No tienes permiso para modificar este docente.");

    mapper.updateEntityFromDto(request, docenteExistente);
    return mapper.toResponseDto(repository.save(docenteExistente));
  }

  @Transactional(readOnly = true)
  public boolean existsDocenteById(Long id) {
    return repository.existsByIdAndActivoTrue(id);
  }

  @Transactional
  public void deleteDocente(Long id) {
    Docente existing = repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Docente con ID " + id + " no encontrado."));
    existing.setActivo(false);
    repository.save(existing);
  }
}

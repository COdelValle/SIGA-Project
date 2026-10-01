package cl.siga.msevaluaciones.service;

import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msevaluaciones.client.AsignaturaClient;
import cl.siga.msevaluaciones.model.entity.Evaluacion;
import cl.siga.msevaluaciones.model.mapper.EvaluacionMapper;
import cl.siga.msevaluaciones.model.specifications.EvaluacionSpecifications;
import cl.siga.msevaluaciones.repository.EvaluacionRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class EvaluacionService {

  private final EvaluacionRepository repository;
  private final EvaluacionMapper mapper;
  private final AsignaturaClient asignaturaClient; // Cliente Feign para validar contra ms-asignaturas

  @Transactional(readOnly = true)
  public EvaluacionResponseDTO getEvaluacionById(Long id) {
    return mapper.toResponseDto(repository.findByIdAndActiveTrue(id)
      .orElseThrow(() -> new ResourceNotFoundException("Evaluación con ID " + id + " no encontrada.")));
  }

  @Transactional(readOnly = true)
  public List<EvaluacionResponseDTO> searchEvaluaciones(String nombre, TipoEvaluacion tipo, Long idAsignatura) {
    Specification<Evaluacion> spec = EvaluacionSpecifications.isActive()
      .and(EvaluacionSpecifications.hasNombre(nombre))
      .and(EvaluacionSpecifications.hasTipo(tipo))
      .and(EvaluacionSpecifications.hasIdAsignatura(idAsignatura));

    return mapper.toResponseDtoList(repository.findAll(spec));
  }

  @Transactional
  public EvaluacionResponseDTO saveEvaluacion(@Valid RegistrarEvaluacionRequestDTO request) {
    if (!asignaturaClient.existsById(request.idAsignatura())) {
      throw new BusinessException("La asignatura con ID " + request.idAsignatura() + " no existe o no está activa.");
    }

    String nombreNormalizado = request.nombre() != null ? request.nombre().trim() : null;
    if (repository.existsByNombreIgnoreCaseAndIdAsignaturaAndActiveTrue(nombreNormalizado, request.idAsignatura())) {
      throw new BusinessException(
        String.format("Ya existe una evaluación llamada '%s' para la asignatura con ID %d.",
          nombreNormalizado, request.idAsignatura())
      );
    }

    validarPonderacionAcumulada(request.idAsignatura(), request.ponderacion(), null);

    return mapper.toResponseDto(repository.save(mapper.toEntity(request)));
  }

  @Transactional
  public EvaluacionResponseDTO updateEvaluacion(Long idEvaluacion, @Valid ActualizarEvaluacionRequestDTO request) {
    Evaluacion existingEvaluacion = repository.findByIdAndActiveTrue(idEvaluacion)
      .orElseThrow(() -> new ResourceNotFoundException("Evaluación con ID " + idEvaluacion + " no encontrada."));

    validarPonderacionAcumulada(existingEvaluacion.getIdAsignatura(), request.ponderacion(), idEvaluacion);

    mapper.updateEntityFromDto(request, existingEvaluacion);
    return mapper.toResponseDto(repository.save(existingEvaluacion));
  }

  @Transactional(readOnly = true)
  public boolean existsEvaluacionById(Long id) {
    return repository.existsByIdAndActiveTrue(id);
  }

  @Transactional
  public void deleteEvaluacion(Long id) {
    Evaluacion existingEvaluacion = repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Evaluación con ID " + id + " no encontrada."));

    existingEvaluacion.setActive(false);
    repository.save(existingEvaluacion);
  }

  /**
   * Valida que la inserción o actualización de una ponderación no supere el 100% en la asignatura.
   */
  private void validarPonderacionAcumulada(Long idAsignatura, Double nuevaPonderacion, Long idEvaluacionActual) {
    List<Evaluacion> evaluacionesAsignatura = repository.findByIdAsignaturaAndActiveTrue(idAsignatura);

    double sumaActual = evaluacionesAsignatura.stream()
      .filter(ev -> idEvaluacionActual == null || !ev.getId().equals(idEvaluacionActual))
      .mapToDouble(Evaluacion::getPonderacion)
      .sum();

    if ((sumaActual + nuevaPonderacion) > 100.0) {
      throw new BusinessException(
        String.format("La suma de las ponderaciones excederá el 100%%. Suma actual (sin considerar esta evaluación): %.1f%%. Nueva ponderación: %.1f%%.",
          sumaActual, nuevaPonderacion)
      );
    }
  }
}

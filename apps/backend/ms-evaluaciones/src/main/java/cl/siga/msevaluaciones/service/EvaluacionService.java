package cl.siga.msevaluaciones.service;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msevaluaciones.client.AsignaturaClient;
import cl.siga.msevaluaciones.mensajeria.PublicadorEvaluacion;
import cl.siga.msevaluaciones.model.entity.Evaluacion;
import cl.siga.msevaluaciones.model.mapper.EvaluacionMapper;
import cl.siga.msevaluaciones.model.specifications.EvaluacionSpecifications;
import cl.siga.msevaluaciones.repository.EvaluacionRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  private final PublicadorEvaluacion publicador; // Productor RabbitMQ (opción 1)

  @Transactional(readOnly = true)
  public EvaluacionResponseDTO getEvaluacionById(Long id) {
    return mapper.toResponseDto(repository.findByIdAndActiveTrue(id)
      .orElseThrow(() -> new ResourceNotFoundException("Evaluación con ID " + id + " no encontrada.")));
  }

  @Transactional(readOnly = true)
  public Page<EvaluacionResponseDTO> searchEvaluaciones(
      String nombre, TipoEvaluacion tipo, Long idCursoAsignatura, Pageable pageable) {
    Specification<Evaluacion> spec = EvaluacionSpecifications.isActive()
      .and(EvaluacionSpecifications.hasNombre(nombre))
      .and(EvaluacionSpecifications.hasTipo(tipo))
      .and(EvaluacionSpecifications.hasIdCursoAsignatura(idCursoAsignatura));

    return repository.findAll(spec, pageable).map(mapper::toResponseDto);
  }

  @Transactional
  public EvaluacionResponseDTO saveEvaluacion(@Valid RegistrarEvaluacionRequestDTO request) {
    validarDictacionCalificable(request.idCursoAsignatura());

    String nombreNormalizado = request.nombre() != null ? request.nombre().trim() : null;
    if (repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrue(nombreNormalizado, request.idCursoAsignatura())) {
      throw new BusinessException(
        String.format("Ya existe una evaluación llamada '%s' para la dictación con ID %d.",
          nombreNormalizado, request.idCursoAsignatura())
      );
    }

    validarPonderacionAcumulada(request.idCursoAsignatura(), request.ponderacion(), null);

    EvaluacionResponseDTO creada = mapper.toResponseDto(repository.save(mapper.toEntity(request)));
    // Productor: avisa a estudiantes y apoderados sin bloquear la respuesta.
    publicador.publicarCreada(creada);
    return creada;
  }

  @Transactional
  public EvaluacionResponseDTO updateEvaluacion(Long idEvaluacion, @Valid ActualizarEvaluacionRequestDTO request) {
    Evaluacion existingEvaluacion = repository.findByIdAndActiveTrue(idEvaluacion)
      .orElseThrow(() -> new ResourceNotFoundException("Evaluación con ID " + idEvaluacion + " no encontrada."));

    String nombreNormalizado = request.nombre() != null ? request.nombre().trim() : null;
    if (nombreNormalizado != null && repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrueAndIdNot(
        nombreNormalizado, existingEvaluacion.getIdCursoAsignatura(), idEvaluacion)) {
      throw new BusinessException(
        String.format("Ya existe una evaluación llamada '%s' para la dictación con ID %d.",
          nombreNormalizado, existingEvaluacion.getIdCursoAsignatura())
      );
    }

    validarPonderacionAcumulada(existingEvaluacion.getIdCursoAsignatura(), request.ponderacion(), idEvaluacion);

    mapper.updateEntityFromDto(request, existingEvaluacion);
    EvaluacionResponseDTO actualizada = mapper.toResponseDto(repository.save(existingEvaluacion));
    // Productor: avisa el cambio de nombre, tipo o ponderación.
    publicador.publicarActualizada(actualizada);
    return actualizada;
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
    EvaluacionResponseDTO eliminada = mapper.toResponseDto(repository.save(existingEvaluacion));
    // Productor: avisa que se canceló la evaluación.
    publicador.publicarEliminada(eliminada);
  }

  /**
   * Valida que la dictación exista, esté activa y su asignatura sea calificable.
   */
  private void validarDictacionCalificable(Long idCursoAsignatura) {
    CursoAsignaturaResponseDTO curso = asignaturaClient.getCursoAsignaturaById(idCursoAsignatura);
    if (curso == null) {
      throw new BusinessException("La dictación con ID " + idCursoAsignatura + " no existe o no está activa.");
    }
    if (!curso.calificable()) {
      throw new BusinessException("La asignatura '" + curso.nombre() + "' no es calificable: no admite evaluaciones.");
    }
  }

  /**
   * Valida que la inserción o actualización de una ponderación no supere el 100% en la dictación.
   */
  private void validarPonderacionAcumulada(Long idCursoAsignatura, Double nuevaPonderacion, Long idEvaluacionActual) {
    List<Evaluacion> evaluacionesDictacion = repository.findActiveByIdCursoAsignaturaForUpdate(idCursoAsignatura);

    double sumaActual = evaluacionesDictacion.stream()
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

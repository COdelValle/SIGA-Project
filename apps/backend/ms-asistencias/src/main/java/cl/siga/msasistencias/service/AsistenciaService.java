package cl.siga.msasistencias.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasistencias.client.AsignaturaClient;
import cl.siga.msasistencias.client.EstudianteClient;
import cl.siga.msasistencias.model.entity.Asistencia;
import cl.siga.msasistencias.model.mapper.AsistenciaMapper;
import cl.siga.msasistencias.model.specifications.AsistenciaSpecifications;
import cl.siga.msasistencias.repository.AsistenciaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class AsistenciaService {
    private final AsistenciaRepository repository;
    private final AsistenciaMapper mapper;
    private final EstudianteClient estudianteClient;
    private final AsignaturaClient asignaturaClient;

    @Transactional(readOnly = true)
    public AsistenciaResponseDTO getAsistenciaById(Long id) {
        return mapper.toResponseDto(repository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asistencia con ID " + id + " no encontrada.")));
    }

    @Transactional(readOnly = true)
    public Page<AsistenciaResponseDTO> searchAsistencias(
            Long idEstudiante,
            Long idCursoAsignatura,
            LocalDate from,
            LocalDate to,
            State estado,
            Pageable pageable) {
        Specification<Asistencia> spec = AsistenciaSpecifications.isActive()
            .and(AsistenciaSpecifications.hasIdEstudiante(idEstudiante))
            .and(AsistenciaSpecifications.hasIdCursoAsignatura(idCursoAsignatura))
            .and(AsistenciaSpecifications.hasFechaGreaterThanOrEqual(from))
            .and(AsistenciaSpecifications.hasFechaLessThanOrEqual(to))
            .and(AsistenciaSpecifications.hasEstado(estado));
        return repository.findAll(spec, pageable).map(mapper::toResponseDto);
    }

    @Transactional
    public AsistenciaResponseDTO saveAsistencia(@Valid RegistrarAsistenciaRequestDTO request) {
        if (!estudianteClient.existsById(request.idEstudiante())) {
            throw new BusinessException("El estudiante no existe: " + request.idEstudiante());
        }
        if (!asignaturaClient.existsById(request.idCursoAsignatura())) {
            throw new BusinessException("La dictación no existe: " + request.idCursoAsignatura());
        }
        if (repository.existsByIdEstudianteAndIdCursoAsignaturaAndFecha(
                request.idEstudiante(), request.idCursoAsignatura(), request.fecha())) {
            throw new BusinessException("Ya existe asistencia para el estudiante "
                + request.idEstudiante() + " en la dictación " + request.idCursoAsignatura()
                + " el " + request.fecha() + ".");
        }

        Asistencia asistencia = mapper.toEntity(request);
        // Presente => No aplica; ausente/atrasado => Pendiente (hasta gestionar justificacion).
        asistencia.setJustificacion(request.estado() == State.PRESENTE
            ? Justificacion.NO_APLICA
            : Justificacion.PENDIENTE);
        asistencia.setActive(true);
        return mapper.toResponseDto(repository.save(asistencia));
    }

    @Transactional
    public AsistenciaResponseDTO updateAsistencia(Long id, @Valid ActualizarAsistenciaRequestDTO request) {
        Asistencia asistencia = repository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asistencia con ID " + id + " no encontrada."));
        mapper.updateEntityFromDto(request, asistencia);
        if (request.estado() != null) {
            // Correccion de la marcacion por parte del docente: se normaliza la
            // justificacion asociada al nuevo estado.
            asistencia.setEstado(request.estado());
            if (request.estado() == State.PRESENTE) {
                asistencia.setJustificacion(Justificacion.NO_APLICA);
            } else if (asistencia.getJustificacion() == Justificacion.NO_APLICA) {
                asistencia.setJustificacion(Justificacion.PENDIENTE);
            }
        }
        return mapper.toResponseDto(repository.save(asistencia));
    }

    @Transactional(readOnly = true)
    public boolean existsAsistenciaById(Long id) {
        return repository.findByIdAndActiveTrue(id).isPresent();
    }

    @Transactional
    public void deleteAsistencia(Long id) {
        Asistencia asistencia = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asistencia con ID " + id + " no encontrada."));
        asistencia.setActive(false);
        repository.save(asistencia);
    }
}

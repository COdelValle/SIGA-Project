package cl.siga.msasignaturas.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.coreshare.dto.asignatura.inscripcion.ActualizarEstadoInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.RegistrarInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.model.InscripcionEstados;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaElectiva;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.mapper.InscripcionMapper;
import cl.siga.msasignaturas.model.specifications.InscripcionSpecifications;
import cl.siga.msasignaturas.repository.InscripcionRepository;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final InscripcionMapper inscripcionMapper;

    @Transactional(readOnly = true)
    public List<InscripcionResponseDTO> buscarInscripciones(Long idAlumno, Long idAsignatura, List<EstadoInscripcion> estados) {
        Specification<Inscripcion> spec = Specification.where(InscripcionSpecifications.hasIdAlumno(idAlumno))
                .and(InscripcionSpecifications.hasIdAsignatura(idAsignatura))
                .and(InscripcionSpecifications.hasEstadoIn(estados));

        return inscripcionMapper.toDtoList(inscripcionRepository.findAll(spec));
    }

    @Transactional(readOnly = true)
    public InscripcionResponseDTO obtenerPorId(Long id) {
        return inscripcionMapper.toDto(inscripcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada con ID: " + id)));
    }

    @Transactional
    public InscripcionResponseDTO registrarInscripcion(RegistrarInscripcionRequestDTO request) {
        // Bloqueo pesimista: serializa las inscripciones concurrentes de la asignatura.
        Asignatura asignatura = asignaturaRepository.findByIdAndActiveTrueForUpdate(request.idAsignatura())
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada o inactiva"));

        if (!(asignatura instanceof AsignaturaElectiva electiva)) {
            throw new BusinessException("Solo se pueden inscribir alumnos en asignaturas electivas");
        }

        Optional<Inscripcion> inscripcionExistente = inscripcionRepository
                .findByIdAlumnoAndAsignaturaId(request.idAlumno(), request.idAsignatura());

        if (inscripcionExistente.isPresent()) {
            Inscripcion inscripcion = inscripcionExistente.get();
            if (InscripcionEstados.OCUPAN_CUPO.contains(inscripcion.getEstado())) {
                throw new BusinessException("El alumno ya posee una inscripción activa o en proceso para esta asignatura");
            }
            
            validarCuposDisponibles(electiva);
            inscripcion.setEstado(EstadoInscripcion.PRE_INSCRITO); 
            return inscripcionMapper.toDto(inscripcionRepository.save(inscripcion));
        }

        // Validación y uso del mapper para crear la entidad
        validarCuposDisponibles(electiva);
        Inscripcion nuevaInscripcion = inscripcionMapper.toEntity(request);
        nuevaInscripcion.setAsignatura(electiva);
        nuevaInscripcion.setEstado(EstadoInscripcion.PRE_INSCRITO); 

        return inscripcionMapper.toDto(inscripcionRepository.save(nuevaInscripcion));
    }

    @Transactional
    public InscripcionResponseDTO actualizarEstado(Long idInscripcion, ActualizarEstadoInscripcionRequestDTO request) {
        Inscripcion inscripcion = inscripcionRepository.findById(idInscripcion)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada"));

        boolean ocupabaCupo = InscripcionEstados.OCUPAN_CUPO.contains(inscripcion.getEstado());
        boolean ocuparaCupo = InscripcionEstados.OCUPAN_CUPO.contains(request.estado());
        
        if (!ocupabaCupo && ocuparaCupo) {
            Asignatura asignatura = asignaturaRepository.findByIdAndActiveTrueForUpdate(inscripcion.getAsignatura().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada o inactiva"));
            if (asignatura instanceof AsignaturaElectiva electiva) {
                validarCuposDisponibles(electiva);
            }
        }

        inscripcion.setEstado(request.estado());
        return inscripcionMapper.toDto(inscripcionRepository.save(inscripcion));
    }

    private void validarCuposDisponibles(AsignaturaElectiva electiva) {
        int inscritosActuales = inscripcionRepository.countByAsignaturaIdAndEstadoIn(
                electiva.getId(), InscripcionEstados.OCUPAN_CUPO);
        
        if (inscritosActuales >= electiva.getCupoMaximo()) {
            throw new BusinessException("No hay cupos disponibles para esta asignatura electiva");
        }
    }
}
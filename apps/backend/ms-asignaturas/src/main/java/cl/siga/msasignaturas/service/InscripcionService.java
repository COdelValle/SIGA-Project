package cl.siga.msasignaturas.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.inscripcion.ActualizarEstadoInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.RegistrarInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.security.SecurityUtils;
import cl.siga.msasignaturas.client.EstudianteClient;
import cl.siga.msasignaturas.model.InscripcionEstados;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;
import cl.siga.msasignaturas.model.mapper.InscripcionMapper;
import cl.siga.msasignaturas.model.specifications.InscripcionSpecifications;
import cl.siga.msasignaturas.repository.InscripcionRepository;
import cl.siga.msasignaturas.repository.asignatura.CursoAsignaturaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final CursoAsignaturaRepository cursoAsignaturaRepository;
    private final InscripcionMapper inscripcionMapper;
    private final EstudianteClient estudianteClient;

    @Transactional(readOnly = true)
    public Page<InscripcionResponseDTO> buscarInscripciones(
            Long idAlumno, Long idCursoAsignatura, List<EstadoInscripcion> estados, Pageable pageable) {
        Specification<Inscripcion> spec = Specification.where(InscripcionSpecifications.hasIdAlumno(idAlumno))
                .and(InscripcionSpecifications.hasIdCursoAsignatura(idCursoAsignatura))
                .and(InscripcionSpecifications.hasEstadoIn(estados));

        return inscripcionRepository.findAll(spec, pageable).map(inscripcionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public InscripcionResponseDTO obtenerPorId(Long id) {
        return inscripcionMapper.toDto(inscripcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada con ID: " + id)));
    }

    @Transactional
    public InscripcionResponseDTO registrarInscripcion(RegistrarInscripcionRequestDTO request) {
        // Un ESTUDIANTE solo puede inscribirse a si mismo; ADMIN puede inscribir a cualquiera.
        validarAlumnoAutenticado(request.idAlumno());

        // Bloqueo pesimista: serializa las inscripciones concurrentes de la dictación.
        CursoAsignatura curso = cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(request.idCursoAsignatura())
                .orElseThrow(() -> new ResourceNotFoundException("Dictación no encontrada o inactiva"));

        if (curso.getCaracter() == CaracterAsignatura.OBLIGATORIA) {
            throw new BusinessException("Solo se pueden inscribir alumnos en asignaturas optativas o electivas");
        }

        Optional<Inscripcion> inscripcionExistente = inscripcionRepository
                .findByIdAlumnoAndCursoAsignaturaId(request.idAlumno(), request.idCursoAsignatura());

        if (inscripcionExistente.isPresent()) {
            Inscripcion inscripcion = inscripcionExistente.get();
            if (InscripcionEstados.OCUPAN_CUPO.contains(inscripcion.getEstado())) {
                throw new BusinessException("El alumno ya posee una inscripción activa o en proceso para esta asignatura");
            }

            validarCuposDisponibles(curso);
            inscripcion.setEstado(EstadoInscripcion.PRE_INSCRITO);
            return inscripcionMapper.toDto(inscripcionRepository.save(inscripcion));
        }

        validarCuposDisponibles(curso);
        Inscripcion nuevaInscripcion = inscripcionMapper.toEntity(request);
        nuevaInscripcion.setCursoAsignatura(curso);
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
            CursoAsignatura curso = cursoAsignaturaRepository
                    .findByIdAndActiveTrueForUpdate(inscripcion.getCursoAsignatura().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dictación no encontrada o inactiva"));
            validarCuposDisponibles(curso);
        }

        inscripcion.setEstado(request.estado());
        return inscripcionMapper.toDto(inscripcionRepository.save(inscripcion));
    }

    private void validarCuposDisponibles(CursoAsignatura curso) {
        if (curso.getCupoMaximo() == null) {
            return;
        }
        int inscritosActuales = inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(
                curso.getId(), InscripcionEstados.OCUPAN_CUPO);

        if (inscritosActuales >= curso.getCupoMaximo()) {
            throw new BusinessException("No hay cupos disponibles para esta asignatura");
        }
    }

    private void validarAlumnoAutenticado(Long idAlumno) {
        if (SecurityUtils.isAdmin()) {
            if (!estudianteClient.existsById(idAlumno)) {
                throw new BusinessException("El estudiante con ID " + idAlumno + " no existe.");
            }
            return;
        }
        if (!SecurityUtils.hasRole("ESTUDIANTE")) {
            return;
        }
        String oid = SecurityUtils.getCurrentUserOid()
                .orElseThrow(() -> new AccessDeniedException("No se pudo identificar al usuario autenticado."));
        Long idAlumnoAutenticado = estudianteClient.getEstudianteByIdUsuario(oid).id();
        if (!idAlumnoAutenticado.equals(idAlumno)) {
            throw new AccessDeniedException("Solo puedes inscribirte a ti mismo.");
        }
    }
}

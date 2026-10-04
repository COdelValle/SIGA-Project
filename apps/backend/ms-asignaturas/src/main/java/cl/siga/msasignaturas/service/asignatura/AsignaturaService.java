package cl.siga.msasignaturas.service.asignatura;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.coreshare.dto.asignatura.ActualizarAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.RegistrarAsignaturaRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.mapper.AsignaturaMapper;
import cl.siga.msasignaturas.model.specifications.AsignaturaSpecifications;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;
import cl.siga.msasignaturas.repository.asignatura.CursoAsignaturaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsignaturaService {

    private final AsignaturaRepository asignaturaRepository;
    private final CursoAsignaturaRepository cursoAsignaturaRepository;
    private final AsignaturaMapper asignaturaMapper;

    @Transactional(readOnly = true)
    public Page<AsignaturaResponseDTO> searchAsignaturas(
            String nombre, AreaAcademica area, Boolean calificable, Pageable pageable) {

        Specification<Asignatura> spec = Specification.where(AsignaturaSpecifications.isActive())
                .and(AsignaturaSpecifications.hasNombre(nombre))
                .and(AsignaturaSpecifications.hasArea(area))
                .and(AsignaturaSpecifications.hasCalificable(calificable));

        return asignaturaRepository.findAll(spec, pageable).map(asignaturaMapper::toDto);
    }

    @Transactional(readOnly = true)
    public AsignaturaResponseDTO obtenerPorId(Long id) {
        return asignaturaMapper.toDto(asignaturaRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura con ID " + id + " no encontrada o inactiva.")));
    }

    @Transactional(readOnly = true)
    public boolean existAsignatura(Long id) {
        return asignaturaRepository.existsByIdAndActiveTrue(id);
    }

    @Transactional
    public AsignaturaResponseDTO crear(RegistrarAsignaturaRequestDTO dto) {
        if (asignaturaRepository.existsByNombreIgnoreCaseAndActiveTrue(dto.nombre())) {
            throw new BusinessException("Ya existe una asignatura activa con ese nombre.");
        }

        // Si fue eliminada lógicamente, se reactiva en vez de chocar con el UNIQUE.
        return asignaturaRepository.findFirstByNombreIgnoreCase(dto.nombre())
                .map(existente -> {
                    existente.setNombre(dto.nombre());
                    existente.setNombreCorto(dto.nombreCorto());
                    existente.setDescripcion(dto.descripcion());
                    existente.setArea(dto.area());
                    existente.setCalificable(dto.calificable());
                    existente.setActive(true);
                    return asignaturaMapper.toDto(asignaturaRepository.save(existente));
                })
                .orElseGet(() -> asignaturaMapper.toDto(asignaturaRepository.save(Asignatura.builder()
                    .nombre(dto.nombre())
                    .nombreCorto(dto.nombreCorto())
                    .descripcion(dto.descripcion())
                    .area(dto.area())
                    .calificable(dto.calificable())
                    .active(true)
                    .build())));
    }

    @Transactional
    public AsignaturaResponseDTO actualizar(Long id, ActualizarAsignaturaRequestDTO dto) {
        Asignatura asignatura = asignaturaRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura con ID " + id + " no encontrada."));

        if (asignaturaRepository.existsByNombreIgnoreCaseAndActiveTrueAndIdNot(dto.nombre(), id)) {
            throw new BusinessException("Ya existe otra asignatura activa con ese nombre.");
        }

        asignaturaMapper.updateEntity(dto, asignatura);
        return asignaturaMapper.toDto(asignaturaRepository.save(asignatura));
    }

    @Transactional
    public void eliminar(Long id) {
        Asignatura asignatura = asignaturaRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura con ID " + id + " no encontrada."));

        if (cursoAsignaturaRepository.existsByAsignaturaIdAndActiveTrue(id)) {
            throw new BusinessException("No se puede eliminar la asignatura porque tiene dictaciones activas.");
        }

        asignatura.setActive(false);
        asignaturaRepository.save(asignatura);
    }
}

package cl.siga.msasignaturas.service.asignatura;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.enums.TipoAsignatura;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.mapper.AsignaturaMapper;
import cl.siga.msasignaturas.model.specifications.AsignaturaSpecifications;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsignaturaService {

    private final AsignaturaRepository asignaturaRepository;
    private final AsignaturaMapper asignaturaMapper;

    @Transactional(readOnly = true)
    public List<AsignaturaResponseDTO> searchAsignaturas(
            String name, TipoAsignatura tipo, Semestre semestre, AreaAcademica area, 
            Long idDocente, Long idClase, Boolean verificarCupos) {
        
        Specification<Asignatura> spec = Specification.where(AsignaturaSpecifications.isActive())
                .and(AsignaturaSpecifications.hasName(name))
                .and(AsignaturaSpecifications.hasTipo(tipo))
                .and(AsignaturaSpecifications.hasSemestre(semestre))
                .and(AsignaturaSpecifications.hasArea(area))
                .and(AsignaturaSpecifications.hasIdDocente(idDocente))
                .and(AsignaturaSpecifications.hasIdClase(idClase))
                .and(AsignaturaSpecifications.hasCuposDisponibles(verificarCupos));

        return asignaturaMapper.toResponseDtoList(asignaturaRepository.findAll(spec));
    }

    @Transactional(readOnly = true)
    public AsignaturaResponseDTO obtenerPorId(Long id) {
        return asignaturaMapper.toResponseDto(asignaturaRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura con ID " + id + " no encontrada o inactiva.")));
    }

    @Transactional(readOnly = true)
    public boolean existAsignatura(Long id) {
        return asignaturaRepository.existsByIdAndActiveTrue(id);
    }

    @Transactional
    public void eliminarAsignatura(Long id) {
        Asignatura asignatura = asignaturaRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura con ID " + id + " no encontrada."));
        
        asignatura.setActive(false);
        asignaturaRepository.save(asignatura);
    }
}
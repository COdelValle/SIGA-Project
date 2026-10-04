package cl.siga.msasignaturas.service.asignatura;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.asignatura.malla.ActualizarMallaCurricularRequestDTO;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.asignatura.malla.RegistrarMallaCurricularRequestDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.entity.asignatura.MallaCurricular;
import cl.siga.msasignaturas.model.mapper.MallaCurricularMapper;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;
import cl.siga.msasignaturas.repository.asignatura.MallaCurricularRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MallaCurricularService {

    private final MallaCurricularRepository mallaCurricularRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final MallaCurricularMapper mallaCurricularMapper;

    @Transactional(readOnly = true)
    public List<MallaCurricularResponseDTO> listar(Nivel nivel, PlanFormacion plan) {
        List<MallaCurricular> filas;
        if (nivel == null) {
            filas = mallaCurricularRepository.findByActiveTrueOrderByNivelAscAsignaturaNombreAsc();
            if (plan != null) {
                filas = filas.stream().filter(fila -> fila.getPlan() == plan).toList();
            }
        } else if (plan == null) {
            filas = mallaCurricularRepository.findByNivelAndActiveTrueOrderByAsignaturaNombreAsc(nivel);
        } else {
            filas = mallaCurricularRepository.findByNivelAndPlanAndActiveTrueOrderByAsignaturaNombreAsc(nivel, plan);
        }
        return filas.stream().map(mallaCurricularMapper::toDto).toList();
    }

    @Transactional
    public MallaCurricularResponseDTO crear(RegistrarMallaCurricularRequestDTO dto) {
        Asignatura asignatura = asignaturaRepository.findByIdAndActiveTrue(dto.idAsignatura())
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada o inactiva."));

        if (mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrue(
                dto.nivel(), dto.idAsignatura(), dto.plan())) {
            throw new BusinessException("La asignatura ya está en la malla de ese nivel y plan.");
        }

        // Si la fila fue eliminada lógicamente, se reactiva en vez de chocar con el UNIQUE.
        return mallaCurricularRepository
                .findFirstByNivelAndAsignaturaIdAndPlan(dto.nivel(), dto.idAsignatura(), dto.plan())
                .map(existente -> {
                    existente.setAsignatura(asignatura);
                    existente.setCaracter(dto.caracter());
                    existente.setHorasSemanales(dto.horasSemanales());
                    existente.setActive(true);
                    return mallaCurricularMapper.toDto(mallaCurricularRepository.save(existente));
                })
                .orElseGet(() -> mallaCurricularMapper.toDto(mallaCurricularRepository.save(
                    MallaCurricular.builder()
                        .nivel(dto.nivel())
                        .asignatura(asignatura)
                        .caracter(dto.caracter())
                        .plan(dto.plan())
                        .horasSemanales(dto.horasSemanales())
                        .active(true)
                        .build())));
    }

    @Transactional
    public MallaCurricularResponseDTO actualizar(Long id, ActualizarMallaCurricularRequestDTO dto) {
        MallaCurricular malla = mallaCurricularRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fila de malla con ID " + id + " no encontrada."));

        if (mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrueAndIdNot(
                malla.getNivel(), malla.getAsignatura().getId(), dto.plan(), id)) {
            throw new BusinessException("Ya existe otra fila para esa asignatura, nivel y plan.");
        }

        malla.setCaracter(dto.caracter());
        malla.setPlan(dto.plan());
        malla.setHorasSemanales(dto.horasSemanales());
        return mallaCurricularMapper.toDto(mallaCurricularRepository.save(malla));
    }

    @Transactional
    public void eliminar(Long id) {
        MallaCurricular malla = mallaCurricularRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fila de malla con ID " + id + " no encontrada."));
        malla.setActive(false);
        mallaCurricularRepository.save(malla);
    }
}

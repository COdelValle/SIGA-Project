package cl.siga.msasignaturas.repository.asignatura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.msasignaturas.model.entity.asignatura.MallaCurricular;

@Repository
public interface MallaCurricularRepository extends JpaRepository<MallaCurricular, Long> {

    List<MallaCurricular> findByNivelAndPlanAndActiveTrueOrderByAsignaturaNombreAsc(Nivel nivel, PlanFormacion plan);

    List<MallaCurricular> findByNivelAndActiveTrueOrderByAsignaturaNombreAsc(Nivel nivel);

    List<MallaCurricular> findByActiveTrueOrderByNivelAscAsignaturaNombreAsc();

    Optional<MallaCurricular> findByIdAndActiveTrue(Long id);

    boolean existsByNivelAndAsignaturaIdAndPlanAndActiveTrue(Nivel nivel, Long idAsignatura, PlanFormacion plan);

    boolean existsByNivelAndAsignaturaIdAndPlanAndActiveTrueAndIdNot(
            Nivel nivel, Long idAsignatura, PlanFormacion plan, Long id);

    Optional<MallaCurricular> findFirstByNivelAndAsignaturaIdAndPlan(
            Nivel nivel, Long idAsignatura, PlanFormacion plan);
}

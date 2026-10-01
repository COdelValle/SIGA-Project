package cl.siga.msevaluaciones.repository;

import cl.siga.msevaluaciones.model.entity.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long>, JpaSpecificationExecutor<Evaluacion> {
  Optional<Evaluacion> findByIdAndActiveTrue(Long id);
  boolean existsByIdAndActiveTrue(Long id);
  List<Evaluacion> findByIdAsignaturaAndActiveTrue(Long idAsignatura);
  boolean existsByIdAsignaturaAndActiveTrue(Long idAsignatura);
  boolean existsByNombreIgnoreCaseAndIdAsignaturaAndActiveTrue(String nombre, Long idAsignatura);
}

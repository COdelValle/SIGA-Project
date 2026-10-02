package cl.siga.msevaluaciones.repository;

import cl.siga.msevaluaciones.model.entity.Evaluacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  boolean existsByNombreIgnoreCaseAndIdAsignaturaAndActiveTrueAndIdNot(String nombre, Long idAsignatura, Long id);

  // Bloquea las evaluaciones activas de la asignatura para serializar el calculo
  // de la ponderacion acumulada (evita que dos altas concurrentes superen el 100%).
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Evaluacion e where e.idAsignatura = :idAsignatura and e.active = true")
  List<Evaluacion> findActiveByIdAsignaturaForUpdate(@Param("idAsignatura") Long idAsignatura);
}

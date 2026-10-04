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
  List<Evaluacion> findByIdCursoAsignaturaAndActiveTrue(Long idCursoAsignatura);
  boolean existsByIdCursoAsignaturaAndActiveTrue(Long idCursoAsignatura);
  boolean existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrue(String nombre, Long idCursoAsignatura);

  boolean existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrueAndIdNot(String nombre, Long idCursoAsignatura, Long id);

  // Bloquea las evaluaciones activas de la dictación para serializar el cálculo
  // de la ponderación acumulada (evita que dos altas concurrentes superen el 100%).
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Evaluacion e where e.idCursoAsignatura = :idCursoAsignatura and e.active = true")
  List<Evaluacion> findActiveByIdCursoAsignaturaForUpdate(@Param("idCursoAsignatura") Long idCursoAsignatura);
}

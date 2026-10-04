package cl.siga.msasignaturas.repository.asignatura;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;
import jakarta.persistence.LockModeType;

@Repository
public interface CursoAsignaturaRepository
        extends JpaRepository<CursoAsignatura, Long>, JpaSpecificationExecutor<CursoAsignatura> {

    Optional<CursoAsignatura> findByIdAndActiveTrue(Long id);

    boolean existsByIdAndActiveTrue(Long id);

    // Bloquea la fila de la dictación para serializar la validación de cupos.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CursoAsignatura c where c.id = :id and c.active = true")
    Optional<CursoAsignatura> findByIdAndActiveTrueForUpdate(@Param("id") Long id);

    boolean existsByAsignaturaIdAndIdClaseAndSemestreAndActiveTrue(
            Long idAsignatura, Long idClase, Semestre semestre);

    boolean existsByAsignaturaIdAndIdClaseAndSemestreAndActiveTrueAndIdNot(
            Long idAsignatura, Long idClase, Semestre semestre, Long id);

    Optional<CursoAsignatura> findFirstByAsignaturaIdAndIdClaseAndSemestre(
            Long idAsignatura, Long idClase, Semestre semestre);

    boolean existsByIdDocenteAndActiveTrue(Long idDocente);

    boolean existsByAsignaturaIdAndActiveTrue(Long idAsignatura);
}

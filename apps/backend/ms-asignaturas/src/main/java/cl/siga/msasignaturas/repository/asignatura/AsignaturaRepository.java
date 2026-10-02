package cl.siga.msasignaturas.repository.asignatura;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import jakarta.persistence.LockModeType;


@Repository
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long>, JpaSpecificationExecutor<Asignatura> {
    
    Optional<Asignatura> findByIdAndActiveTrue(Long id);
    
    boolean existsByIdAndActiveTrue(Long id);

    // Bloquea la fila de la asignatura para serializar la validacion de cupos
    // (evita que dos inscripciones concurrentes superen el cupo maximo).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asignatura a where a.id = :id and a.active = true")
    Optional<Asignatura> findByIdAndActiveTrueForUpdate(@Param("id") Long id);

    // Valida si un docente ya tiene asignaturas activas asignadas
    boolean existsByIdDocenteAndActiveTrue(Long idDocente);
    
    // Para evitar crear asignaturas duplicadas con el mismo nombre exacto y semestre
    boolean existsByNameIgnoreCaseAndSemestreAndActiveTrue(String name, Semestre semestre);
}
package cl.siga.msasignaturas.repository.asignatura;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;


@Repository
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long>, JpaSpecificationExecutor<Asignatura> {
    
    Optional<Asignatura> findByIdAndActiveTrue(Long id);
    
    boolean existsByIdAndActiveTrue(Long id);

    // Valida si un docente ya tiene asignaturas activas asignadas
    boolean existsByIdDocenteAndActiveTrue(Long idDocente);
    
    // Para evitar crear asignaturas duplicadas con el mismo nombre exacto y semestre
    boolean existsByNameIgnoreCaseAndSemestreAndActiveTrue(String name, Semestre semestre);
}
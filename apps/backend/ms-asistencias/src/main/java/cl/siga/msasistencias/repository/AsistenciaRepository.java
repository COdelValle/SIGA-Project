package cl.siga.msasistencias.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.msasistencias.model.entity.Asistencia;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long>, JpaSpecificationExecutor<Asistencia> {

    Optional<Asistencia> findByIdAndActiveTrue(Long id);

    boolean existsByIdEstudianteAndIdCursoAsignaturaAndFecha(Long idEstudiante, Long idCursoAsignatura, LocalDate fecha);

    // Conteo de faltas (AUSENTE) del estudiante en la dictación durante un rango de fechas.
    long countByIdEstudianteAndIdCursoAsignaturaAndFechaBetweenAndEstadoAndActiveTrue(
            Long idEstudiante, Long idCursoAsignatura, LocalDate desde, LocalDate hasta, State estado);

    // Conteo total de registros del estudiante en la dictación durante un rango de fechas.
    long countByIdEstudianteAndIdCursoAsignaturaAndFechaBetweenAndActiveTrue(
            Long idEstudiante, Long idCursoAsignatura, LocalDate desde, LocalDate hasta);
}

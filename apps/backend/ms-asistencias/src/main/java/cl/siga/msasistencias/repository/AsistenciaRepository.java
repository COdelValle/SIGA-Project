package cl.siga.msasistencias.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import cl.siga.msasistencias.model.entity.Asistencia;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long>, JpaSpecificationExecutor<Asistencia> {

    Optional<Asistencia> findByIdAndActiveTrue(Long id);

    boolean existsByIdEstudianteAndIdCursoAsignaturaAndFecha(Long idEstudiante, Long idCursoAsignatura, LocalDate fecha);
}

package cl.siga.msasignaturas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.msasignaturas.model.entity.Inscripcion;

@Repository 
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long>, JpaSpecificationExecutor<Inscripcion> {
    
    // Busca si ya existe un registro previo para el alumno y la asignatura
    Optional<Inscripcion> findByIdAlumnoAndAsignaturaId(Long idAlumno, Long idAsignatura);
    
    // Cuenta los cupos ocupados (ignorando CANCELADO o EN_ESPERA)
    int countByAsignaturaIdAndEstadoIn(Long idAsignatura, List<EstadoInscripcion> estadosOcupados);
    
    List<Inscripcion> findByIdAlumno(Long idAlumno);
    
    List<Inscripcion> findByAsignaturaId(Long idAsignatura);
}
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

    Optional<Inscripcion> findByIdAlumnoAndCursoAsignaturaId(Long idAlumno, Long idCursoAsignatura);

    int countByCursoAsignaturaIdAndEstadoIn(Long idCursoAsignatura, List<EstadoInscripcion> estadosOcupados);

    List<Inscripcion> findByIdAlumno(Long idAlumno);

    List<Inscripcion> findByCursoAsignaturaId(Long idCursoAsignatura);
}

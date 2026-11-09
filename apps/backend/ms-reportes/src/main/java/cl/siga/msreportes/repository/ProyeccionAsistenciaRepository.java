package cl.siga.msreportes.repository;

import cl.siga.msreportes.model.entity.ProyeccionAsistencia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyeccionAsistenciaRepository extends JpaRepository<ProyeccionAsistencia, Long> {
    Optional<ProyeccionAsistencia> findByIdEstudianteAndIdCursoAsignatura(
        Long idEstudiante, Long idCursoAsignatura);
}

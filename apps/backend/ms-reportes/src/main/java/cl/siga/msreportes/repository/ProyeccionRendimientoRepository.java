package cl.siga.msreportes.repository;

import cl.siga.msreportes.model.entity.ProyeccionRendimiento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyeccionRendimientoRepository extends JpaRepository<ProyeccionRendimiento, Long> {
    Optional<ProyeccionRendimiento> findByIdEstudianteAndIdEvaluacion(
        Long idEstudiante, Long idEvaluacion);
}

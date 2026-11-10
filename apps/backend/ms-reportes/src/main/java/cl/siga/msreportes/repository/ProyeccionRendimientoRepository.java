package cl.siga.msreportes.repository;

import cl.siga.msreportes.model.entity.ProyeccionRendimiento;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyeccionRendimientoRepository extends JpaRepository<ProyeccionRendimiento, Long> {
    Optional<ProyeccionRendimiento> findByIdEstudianteAndIdEvaluacion(
        Long idEstudiante, Long idEvaluacion);

    List<ProyeccionRendimiento> findByIdEstudianteAndIdCursoAsignatura(
        Long idEstudiante, Long idCursoAsignatura);

    @Query("""
        select p from ProyeccionRendimiento p
        where (:estudiante is null or p.idEstudiante = :estudiante)
          and (:curso is null or p.idCursoAsignatura = :curso)
          and (:from is null or p.actualizadoEn >= :from)
          and (:to is null or p.actualizadoEn <= :to)
        order by p.actualizadoEn desc, p.id desc
        """)
    List<ProyeccionRendimiento> buscar(
        @Param("estudiante") Long estudiante, @Param("curso") Long curso,
        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

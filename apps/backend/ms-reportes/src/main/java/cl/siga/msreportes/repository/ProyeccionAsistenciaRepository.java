package cl.siga.msreportes.repository;

import cl.siga.msreportes.model.entity.ProyeccionAsistencia;
import java.util.Optional;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyeccionAsistenciaRepository extends JpaRepository<ProyeccionAsistencia, Long> {
    Optional<ProyeccionAsistencia> findByIdEstudianteAndIdCursoAsignatura(
        Long idEstudiante, Long idCursoAsignatura);

    @Query("""
        select p from ProyeccionAsistencia p
        where (:estudiante is null or p.idEstudiante = :estudiante)
          and (:curso is null or p.idCursoAsignatura = :curso)
          and (:from is null or p.fecha >= :from)
          and (:to is null or p.fecha <= :to)
        order by p.fecha desc, p.id desc
        """)
    List<ProyeccionAsistencia> buscar(
        @Param("estudiante") Long estudiante, @Param("curso") Long curso,
        @Param("from") LocalDate from, @Param("to") LocalDate to);
}

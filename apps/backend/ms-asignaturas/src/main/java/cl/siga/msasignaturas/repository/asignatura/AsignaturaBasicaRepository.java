package cl.siga.msasignaturas.repository.asignatura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaBasica;

@Repository 
public interface AsignaturaBasicaRepository extends JpaRepository<AsignaturaBasica, Long>, JpaSpecificationExecutor<AsignaturaBasica> {

    Optional<AsignaturaBasica> findByIdAndActiveTrue(Long id);

    // Muy útil para saber si una clase en específico tiene asignaturas creadas
    boolean existsByIdClaseAndActiveTrue(Long idClase);

    // Retorna todas las asignaturas básicas asignadas a una clase específica (ej. 3ro Medio)
    List<AsignaturaBasica> findAllByIdClaseAndActiveTrue(Long idClase);

    boolean existsByNameIgnoreCaseAndSemestreAndIdClaseAndActiveTrue(String name, Semestre semestre, Long idClase);
}
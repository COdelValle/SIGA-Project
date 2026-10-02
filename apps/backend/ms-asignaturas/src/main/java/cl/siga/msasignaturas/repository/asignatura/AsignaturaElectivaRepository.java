package cl.siga.msasignaturas.repository.asignatura;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaElectiva;

@Repository 
public interface AsignaturaElectivaRepository extends JpaRepository<AsignaturaElectiva, Long>, JpaSpecificationExecutor<AsignaturaElectiva> {
    Optional<AsignaturaElectiva> findByIdAndActiveTrue(Long id);

    boolean existsByNameIgnoreCaseAndSemestreAndActiveTrue(String name, Semestre semestre);

    boolean existsByNameIgnoreCaseAndSemestreAndActiveTrueAndIdNot(String name, Semestre semestre, Long id);

    Optional<AsignaturaElectiva> findFirstByNameIgnoreCaseAndSemestre(String name, Semestre semestre);
}
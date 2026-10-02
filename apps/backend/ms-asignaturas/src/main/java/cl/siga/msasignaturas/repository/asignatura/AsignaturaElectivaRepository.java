package cl.siga.msasignaturas.repository.asignatura;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaElectiva;

@Repository 
public interface AsignaturaElectivaRepository extends JpaRepository<AsignaturaElectiva, Long>, JpaSpecificationExecutor<AsignaturaElectiva> {
    Optional<AsignaturaElectiva> findByIdAndActiveTrue(Long id);
}
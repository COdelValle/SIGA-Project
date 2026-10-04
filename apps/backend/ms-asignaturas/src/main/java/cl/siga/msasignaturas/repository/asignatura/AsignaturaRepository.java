package cl.siga.msasignaturas.repository.asignatura;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;

@Repository
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long>, JpaSpecificationExecutor<Asignatura> {

    Optional<Asignatura> findByIdAndActiveTrue(Long id);

    boolean existsByIdAndActiveTrue(Long id);

    boolean existsByNombreIgnoreCaseAndActiveTrue(String nombre);

    boolean existsByNombreIgnoreCaseAndActiveTrueAndIdNot(String nombre, Long id);

    Optional<Asignatura> findFirstByNombreIgnoreCase(String nombre);
}

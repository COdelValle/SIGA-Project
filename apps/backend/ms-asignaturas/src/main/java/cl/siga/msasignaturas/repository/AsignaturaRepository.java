package cl.siga.msasignaturas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.siga.msasignaturas.model.entity.Asignatura;

@Repository 
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long>{
    Optional<Asignatura> findByName(String name);
    boolean existsByName(String name);
}

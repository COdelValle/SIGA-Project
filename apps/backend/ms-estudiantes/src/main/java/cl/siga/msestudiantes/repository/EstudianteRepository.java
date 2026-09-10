package cl.siga.msestudiantes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import cl.siga.msestudiantes.model.entity.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long>, JpaSpecificationExecutor<Estudiante> {
    Optional<Estudiante> findByRut(String rut);
    boolean existsByRut(String rut);
    Optional<Estudiante> findByIdUsuario(String idUsuario);
    boolean existsByIdUsuario(String idUsuario);
}

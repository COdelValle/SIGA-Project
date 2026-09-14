package cl.siga.msestudiantes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.coreshare.dto.estudiante.enums.State;
import cl.siga.msestudiantes.model.entity.Estudiante;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long>, JpaSpecificationExecutor<Estudiante> {
    Optional<Estudiante> findByRut(String rut);
    boolean existsByRut(String rut);
    Optional<Estudiante> findByIdUsuario(String idUsuario);
    boolean existsByIdUsuario(String idUsuario);
    Optional<Estudiante> findByIdAndStateNot(Long id, State state);
    Optional<Estudiante> findByIdUsuarioAndStateNot(String idUsuario, State state);
    boolean existsByIdAndStateNot(Long id, State state);
}

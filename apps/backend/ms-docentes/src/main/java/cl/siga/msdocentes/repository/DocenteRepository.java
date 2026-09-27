package cl.siga.msdocentes.repository;

import cl.siga.msdocentes.model.entity.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long>, JpaSpecificationExecutor<Docente> {
  Optional<Docente> findByRut(String rut);
  boolean existsByRut(String rut);
  Optional<Docente> findByIdUsuario(String idUsuario);
  boolean existsByIdUsuario(String idUsuario);
  Optional<Docente> findByIdAndActivoTrue(Long id);
  Optional<Docente> findByIdUsuarioAndActivoTrue(String idUsuario);
  boolean existsByIdAndActivoTrue(Long id);
}

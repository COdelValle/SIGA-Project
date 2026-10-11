package cl.siga.msapoderados.repository;

import cl.siga.msapoderados.model.entity.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApoderadoRepository extends JpaRepository<Apoderado, Long>, JpaSpecificationExecutor<Apoderado> {
  Optional<Apoderado> findByRut(String rut);
  boolean existsByRut(String rut);
  Optional<Apoderado> findByIdUsuario(String idUsuario);
  boolean existsByIdUsuario(String idUsuario);
  Optional<Apoderado> findByIdAndActivoTrue(Long id);
  Optional<Apoderado> findByIdUsuarioAndActivoTrue(String idUsuario);
  boolean existsByIdAndActivoTrue(Long id);
}

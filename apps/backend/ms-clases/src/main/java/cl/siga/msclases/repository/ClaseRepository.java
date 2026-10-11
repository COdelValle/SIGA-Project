package cl.siga.msclases.repository;

import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.msclases.model.entity.Clase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClaseRepository extends JpaRepository<Clase, Long>, JpaSpecificationExecutor<Clase> {
  Optional<Clase> findByIdAndActiveTrue(Long id);
  boolean existsByIdAndActiveTrue(Long id);

  boolean existsByNivelAndLetraAndAnioAcademico(Nivel nivel, String letra, Integer anioAcademico);
  Optional<Clase> findByNivelAndLetraAndAnioAcademico(Nivel nivel, String letra, Integer anioAcademico);

  boolean existsByIdDocenteJefeAndActiveTrue(Long idDocenteJefe);

  boolean existsByIdDocenteJefeAndActiveTrueAndIdNot(Long idDocenteJefe, Long id);
}

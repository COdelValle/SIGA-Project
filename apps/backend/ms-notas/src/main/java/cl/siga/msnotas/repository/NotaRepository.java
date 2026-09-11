package cl.siga.msnotas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.msnotas.model.entity.Nota;

@Repository 
public interface NotaRepository extends JpaRepository<Nota, Long>, JpaSpecificationExecutor<Nota> {
}

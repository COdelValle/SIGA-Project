package cl.siga.msdocentes.repository;

import cl.siga.msdocentes.model.entity.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Long> {
  List<Certificado> findByDocenteId(Long docenteId);
  long countByDocenteId(Long docenteId);
}

package cl.siga.msreportes.repository;

import cl.siga.msreportes.model.entity.AlertaRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaRiesgoRepository extends JpaRepository<AlertaRiesgo, Long> {
    boolean existsByClaveIdempotencia(String claveIdempotencia);
}

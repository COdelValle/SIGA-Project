package cl.siga.msreportes.repository;

import cl.siga.msreportes.model.entity.EventoReporteProcesado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoReporteProcesadoRepository extends JpaRepository<EventoReporteProcesado, Long> {
    boolean existsByIdEvento(String idEvento);
}

package cl.siga.msusuariosauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.siga.msusuariosauth.model.entity.ProcessedRegistrationEvent;

public interface ProcessedRegistrationEventRepository extends JpaRepository<ProcessedRegistrationEvent, String> {
}

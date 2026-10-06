package cl.siga.msusuariosauth.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.siga.msusuariosauth.model.entity.OutboxEventState;
import cl.siga.msusuariosauth.model.entity.RegistrationOutboxEvent;

public interface RegistrationOutboxEventRepository extends JpaRepository<RegistrationOutboxEvent, Long> {

    List<RegistrationOutboxEvent> findTop20ByStateAndNextAttemptAtLessThanEqualOrderByIdAsc(
            OutboxEventState state, OffsetDateTime ahora);
}

package cl.siga.msusuariosauth.model.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Outbox transaccional del registro asíncrono: el evento se persiste en la
 * misma transacción que el cambio de estado y un publicador programado lo
 * envía a RabbitMQ con confirmación del broker y reintentos con backoff.
 */
@Entity
@Table(name = "user_registration_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationOutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", length = 36, nullable = false, unique = true, updatable = false)
    private String eventId;

    @Column(name = "process_id", length = 36)
    private String processId;

    @Column(name = "exchange", length = 120, nullable = false, updatable = false)
    private String exchange;

    @Column(name = "routing_key", length = 120, nullable = false, updatable = false)
    private String routingKey;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private OutboxEventState state;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (state == null) {
            state = OutboxEventState.PENDIENTE;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }
    }
}

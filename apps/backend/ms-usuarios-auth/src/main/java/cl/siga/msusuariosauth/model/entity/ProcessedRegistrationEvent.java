package cl.siga.msusuariosauth.model.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "processed_registration_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedRegistrationEvent {
    @Id
    @Column(name = "event_id", length = 120, nullable = false, updatable = false)
    private String eventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}

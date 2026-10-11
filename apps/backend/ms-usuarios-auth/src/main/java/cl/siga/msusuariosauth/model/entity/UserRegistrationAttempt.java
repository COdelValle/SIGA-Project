package cl.siga.msusuariosauth.model.entity;

import java.time.OffsetDateTime;

import cl.siga.coreshare.dto.usuario.enums.RegistrationStepType;
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

@Entity
@Table(name = "user_registration_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "process_id", length = 36, nullable = false)
    private String processId;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30)
    private RegistrationStepType stepType;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}

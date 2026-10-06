package cl.siga.msusuariosauth.model.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepState;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_registration_processes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationProcess {
    @Id
    @Column(name = "process_id", length = 36, nullable = false, updatable = false)
    private String processId;

    @Column(name = "event_id", length = 36, nullable = false, unique = true, updatable = false)
    private String eventId;

    @Column(name = "correlation_id", length = 100, nullable = false)
    private String correlationId;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_role", nullable = false, length = 50)
    private Rol requestedRole;

    /** Oid del administrador que inició el proceso (dueño de la credencial). */
    @Column(name = "created_by", length = 36)
    private String createdBy;

    /** Correo alternativo para la futura notificación de credenciales (opción 3). */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "user_id", length = 36)
    private String userId;

    /** Payload del rol serializado (JSON) para reconstruir el evento de dominio. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "role_data")
    private String roleData;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private RegistrationProcessState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "azure_state", nullable = false, length = 30)
    private RegistrationStepState azureState;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain_state", nullable = false, length = 30)
    private RegistrationStepState domainState;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** Clave temporal cifrada (AES-GCM); se borra al entregarse. */
    @Column(name = "credential_ciphertext", length = 512)
    private String credentialCiphertext;

    @Column(name = "credential_iv", length = 64)
    private String credentialIv;

    @Column(name = "credential_expires_at")
    private OffsetDateTime credentialExpiresAt;

    @Column(name = "credential_retrieved_at")
    private OffsetDateTime credentialRetrievedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (state == null) {
            state = RegistrationProcessState.PENDIENTE;
        }
        if (azureState == null) {
            azureState = RegistrationStepState.PENDIENTE;
        }
        if (domainState == null) {
            domainState = RegistrationStepState.PENDIENTE;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

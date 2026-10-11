package cl.siga.msusuariosauth.model.entity;

import java.time.LocalDateTime;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateInvitacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Invitación de acceso por correo (UPN de Entra ID). Permite pre-registrar a
 * un usuario sin conocer su {@code oid}: este se vincula en el primer login.
 */
@Entity
@Table(name = "invitaciones_usuarios")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvitacionUsuario {
    @Id
    @Email(message = "El correo electrónico debe tener un formato válido")
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
    @Column(name = "email", length = 255, nullable = false, updatable = false)
    private String email;

    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 50)
    private Rol rol;

    @NotNull(message = "El estado de la invitación es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 50)
    private StateInvitacion state;

    @Column(name = "invited_by", length = 36)
    private String invitedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "bound_at")
    private LocalDateTime boundAt;

    @PrePersist
    public void normalizeData() {
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
        if (this.state == null) {
            this.state = StateInvitacion.INVITADO;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}

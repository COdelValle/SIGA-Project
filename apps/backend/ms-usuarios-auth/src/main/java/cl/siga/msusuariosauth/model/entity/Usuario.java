package cl.siga.msusuariosauth.model.entity;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity 
@Table(
    name = "usuarios",
    indexes = {
        @Index(name = "idx_usuario_email", columnList = "email", unique = true)
    }
)
@Getter 
@Setter 
@Builder 
@AllArgsConstructor 
@NoArgsConstructor 
public class Usuario {
    @Id
    @NotBlank (message = "El ID es obligatorio")
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size (max = 255, message = "El correo no puede superar los 255 caracteres")
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 50, updatable = false)
    private Rol rol;

    @NotNull(message = "El estado del usuario es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 50)
    private StateUsuario state;

    @PrePersist
    @PreUpdate
    public void normalizeData() {
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }
}

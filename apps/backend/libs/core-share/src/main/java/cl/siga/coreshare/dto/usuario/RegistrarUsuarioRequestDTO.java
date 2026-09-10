package cl.siga.coreshare.dto.usuario;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarUsuarioRequestDTO(
    @NotBlank (message = "El ID de usuario de Azure es obligatorio")
    @Size (min = 36, max = 36, message = "El ID de Azure debe tener exactamente 36 caracteres")
    String id,

    @Email (message = "El correo electrónico debe ser válido")
    @NotBlank (message = "El correo electrónico es obligatorio")
    String email,

    @NotNull(message = "El rol es obligatorio")
    Rol rol
) {
}
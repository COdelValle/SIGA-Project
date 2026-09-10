package cl.siga.coreshare.dto.usuario;

import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ActualizarUsuarioRequestDTO(
    @Email (message = "El correo electrónico debe ser válido")
    @NotBlank (message = "El correo electrónico es obligatorio")
    String email,

    @NotNull(message = "El estado del usuario es obligatorio")
    StateUsuario state
) {
}

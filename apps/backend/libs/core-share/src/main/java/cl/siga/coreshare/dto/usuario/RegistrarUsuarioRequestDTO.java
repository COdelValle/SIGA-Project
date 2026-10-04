package cl.siga.coreshare.dto.usuario;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Registro de un usuario. El camino recomendado es enviar solo el correo y el
 * rol: si Microsoft Graph está configurado se resuelve el {@code oid}
 * automáticamente y, si no, se crea una invitación que se vincula en el primer
 * inicio de sesión. El {@code id} explícito se mantiene por compatibilidad y
 * está deprecado: ya no es necesario copiar el oid a mano.
 */
public record RegistrarUsuarioRequestDTO(
    @Pattern (
        regexp = "^\\s*$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "El ID de Azure debe ser un UUID válido"
    )
    String id,

    @Email (message = "El correo electrónico debe ser válido")
    @NotBlank (message = "El correo electrónico es obligatorio")
    String email,

    @NotNull(message = "El rol es obligatorio")
    Rol rol
) {
}

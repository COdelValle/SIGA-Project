package cl.siga.coreshare.dto.usuario;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Alta controlada de un usuario por correo: el administrador no ingresa el
 * object id (oid) de Entra ID; este se vincula automáticamente en el primer
 * inicio de sesión del invitado.
 */
public record InvitacionUsuarioRequestDTO(
    @Email (message = "El correo electrónico debe ser válido")
    @NotBlank (message = "El correo electrónico es obligatorio")
    String email,

    @NotNull (message = "El rol es obligatorio")
    Rol rol
) {
}

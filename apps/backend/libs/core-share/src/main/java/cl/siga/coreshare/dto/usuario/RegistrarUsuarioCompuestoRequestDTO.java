package cl.siga.coreshare.dto.usuario;

import java.util.Map;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrarUsuarioCompuestoRequestDTO(
    @Email(message = "El correo electrónico debe ser válido")
    @NotBlank(message = "El correo electrónico es obligatorio")
    String email,

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 255, message = "El nombre completo no puede superar los 255 caracteres")
    String fullName,

    @NotNull(message = "El rol es obligatorio")
    Rol requestedRole,

    @Pattern(
        regexp = "^\\s*$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "El ID de Azure debe ser un UUID válido"
    )
    String azureUserId,

    @NotNull(message = "Los datos específicos del rol son obligatorios")
    Map<String, Object> roleData
) {
}

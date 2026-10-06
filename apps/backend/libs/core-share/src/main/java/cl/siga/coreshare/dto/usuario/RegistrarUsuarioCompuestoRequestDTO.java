package cl.siga.coreshare.dto.usuario;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de registro compuesto (usuario + perfil de rol). El alta en Entra
 * ID y la creación del perfil ocurren de forma asíncrona: la respuesta es un
 * proceso consultable por {@code processId}.
 *
 * <p>El correo y el nombre completo son opcionales: si no se envían, el servicio
 * los deriva de los nombres del rol (correo {@code nombre.apellido@dominio},
 * con resolución de colisiones).</p>
 */
public record RegistrarUsuarioCompuestoRequestDTO(
    @Email(message = "El correo electrónico debe ser válido")
    @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
    String email,

    @Size(max = 255, message = "El nombre completo no puede superar los 255 caracteres")
    String fullName,

    @NotNull(message = "El rol es obligatorio")
    Rol requestedRole,

    @Pattern(
        regexp = "^\\s*$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "El ID de Azure debe ser un UUID válido"
    )
    String azureUserId,

    @Email(message = "El correo de contacto debe ser válido")
    @Size(max = 255, message = "El correo de contacto no puede superar los 255 caracteres")
    String contactEmail,

    @NotNull(message = "Los datos específicos del rol son obligatorios")
    @Valid
    DatosRegistroRolDTO roleData
) {
}

package cl.siga.coreshare.dto.apoderado;

import cl.siga.coreshare.validation.RUT;
import jakarta.validation.constraints.*;

public record RegistrarAdoderadoRequestDTO(
    @NotBlank(message = "El ID de usuario de Azure es obligatorio")
    @Size(min = 36, max = 36, message = "El ID de Azure debe tener exactamente 36 caracteres")
    String idUsuario,

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50)
    String firstName,

    @Size(min = 0, max = 50)
    String middleName,

    @NotBlank(message = "El primer apellido es requerido")
    @Size(min = 2, max = 50)
    String firstSurname,

    @Size(min = 2, max = 50)
    String secondSurname,
    
    @NotBlank(message = "El RUT es requerido")
    @RUT
    String rut
) {

}

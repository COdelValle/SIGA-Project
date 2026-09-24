package cl.siga.coreshare.dto.apoderado;

import java.util.List;

import cl.siga.coreshare.validation.Phone;
import jakarta.validation.constraints.*;

public record ActualizarApoderadoRequestDTO(
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

    @NotEmpty(message = "Debe tener al menos un teléfono")
    List<@Phone(message = "El teléfono debe tener un formato válido") String> telefonos
) {

}

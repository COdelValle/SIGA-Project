package cl.siga.coreshare.dto.estudiante;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.*;

public record ActualizarEstudianteRequestDTO(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50)
    String firstName,

    @Size(min = 0, max = 50)
    String middleName,

    @NotBlank(message = "El apellido es requerido")
    @Size(min = 2, max = 100)
    String lastName,

    @NotNull(message = "Fecha de nacimiento requerida")
    @Past
    LocalDate birthDate,

    List<String> allergies,

    @NotNull(message = "El estado del usuario es obligatorio")
    String state
) {
}

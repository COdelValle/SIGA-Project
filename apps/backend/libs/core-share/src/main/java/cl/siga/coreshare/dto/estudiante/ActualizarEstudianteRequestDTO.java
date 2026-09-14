package cl.siga.coreshare.dto.estudiante;

import java.time.LocalDate;
import java.util.List;

import cl.siga.coreshare.dto.estudiante.enums.State;
import jakarta.validation.constraints.*;

public record ActualizarEstudianteRequestDTO(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50)
    String firstName,

    @Size(min = 0, max = 50)
    String middleName,

    @NotBlank(message = "El apellido es requerido")
    @Size(min = 2, max = 50)
    String firstSurname,

    @Size(min = 2, max = 50)
    String secondSurname,

    @NotNull(message = "Fecha de nacimiento requerida")
    @Past
    LocalDate birthDate,

    List<String> allergies,

    @NotNull(message = "El estado del usuario es obligatorio")
    State state
) {
}

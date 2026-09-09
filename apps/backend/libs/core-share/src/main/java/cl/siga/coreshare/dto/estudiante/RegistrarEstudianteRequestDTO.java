package cl.siga.coreshare.dto.estudiante;

import java.time.LocalDate;
import java.util.List;

import cl.siga.coreshare.validation.RUT;
import jakarta.validation.constraints.*;

public record RegistrarEstudianteRequestDTO(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50)
    String firstName,

    @Size(min = 0, max = 50)
    String middleName,

    @NotBlank(message = "El apellido es requerido")
    @Size(min = 2, max = 100)
    String lastName,
    
    @NotBlank(message = "El RUT es requerido")
    @RUT
    String rut,

    @NotNull(message = "Fecha de nacimiento requerida")
    @Past
    LocalDate birthDate,

    List<String> allergies,

    @NotNull(message = "El ID de usuario es obligatorio")
    Long idUsuario
) {
}

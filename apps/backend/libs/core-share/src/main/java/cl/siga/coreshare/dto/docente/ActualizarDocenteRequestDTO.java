package cl.siga.coreshare.dto.docente;

import java.time.LocalDate;

import cl.siga.coreshare.dto.docente.enums.AreaAcademica;
import jakarta.validation.constraints.*;

public record ActualizarDocenteRequestDTO(
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

    @NotNull(message = "Fecha de contratación requerida")
    @Past
    LocalDate fechaContratacion,
    
    @NotNull(message = "El área académica es requerida")
    AreaAcademica area
) {

}

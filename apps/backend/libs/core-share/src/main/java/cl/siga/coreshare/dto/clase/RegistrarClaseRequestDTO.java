package cl.siga.coreshare.dto.clase;

import cl.siga.coreshare.dto.clase.enums.Nivel;
import jakarta.validation.constraints.*;

public record RegistrarClaseRequestDTO(
    @NotNull(message = "El nivel es obligatorio")
    Nivel nivel,
 
    @NotBlank (message = "La letra es obligatoria")
    @Pattern(regexp = "^[A-ZÑ]$", message = "La letra debe ser un único carácter alfabético (A-Z)")
    String letra,
 
    @NotNull(message = "El año académico es obligatorio")
    @Min(value = 2000, message = "El año académico debe ser igual o mayor a 2000")
    Integer anioAcademico,
 
    Long idDocenteJefe
) {
}

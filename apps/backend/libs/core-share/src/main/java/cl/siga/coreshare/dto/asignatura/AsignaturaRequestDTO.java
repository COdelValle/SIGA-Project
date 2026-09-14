package cl.siga.coreshare.dto.asignatura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AsignaturaRequestDTO(
    @NotBlank (message = "El nombre es requerido")
    @Size (min = 2, max = 50)
    String name,

    @NotBlank (message = "La descripción es requerida")
    @Size (min = 2, max = 150)
    String description
) {
}
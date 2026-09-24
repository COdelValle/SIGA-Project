package cl.siga.coreshare.dto.apoderado.parentesco;

import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import jakarta.validation.constraints.NotNull;

public record ParentescoEstudianteDTO(
    @NotNull (message = "El ID del estudiante es obligatorio")
    Long idEstudiante,

    @NotNull (message = "El parentesco es obligatorio")
    Parentesco parentesco
) {

}

package cl.siga.coreshare.dto.notas;

import cl.siga.coreshare.validation.ChileanGrade;
import jakarta.validation.constraints.NotNull;

public record RegistrarNotaRequestDTO(
    @NotNull(message = "El ID del estudiante es obligatorio")
    Long idEstudiante,

    @NotNull (message = "El ID de la evaluación es obligatorio")
    Long idEvaluacion,

    @NotNull (message = "La nota es obligatoria")
    @ChileanGrade (message = "La nota debe estar entre 1.0 y 7.0")
    Double score
) {
}

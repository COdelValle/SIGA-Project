package cl.siga.coreshare.dto.notas;

import cl.siga.coreshare.validation.ChileanGrade;
import jakarta.validation.constraints.NotNull;

public record ActualizarNotaRequestDTO(
    @NotNull (message = "La nota es obligatoria")
    @ChileanGrade (message = "La nota debe estar entre 1.0 y 7.0")
    Double score
) {
}

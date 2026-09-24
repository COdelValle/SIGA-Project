package cl.siga.coreshare.dto.asistencia;

import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarAsistenciaRequestDTO(
    @NotNull (message = "La justificación es obligatoria")
    Justificacion justificacion,

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
    String observacion
) {

}

package cl.siga.coreshare.dto.asistencia;

import java.time.LocalDate;

import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import jakarta.validation.constraints.*;

public record RegistrarAsistenciaRequestDTO(
    @NotNull (message = "El ID del estudiante es obligatorio")
    Long idEstudiante,

    @NotNull (message = "El ID del asignatura es obligatorio")
    Long idAsignatura,

    @NotNull (message = "La fecha es obligatoria")
    @PastOrPresent (message = "La fecha no puede ser futura")
    LocalDate fecha,

    @NotNull (message = "El estado es obligatorio")
    State estado,

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
    String observacion
) {

}

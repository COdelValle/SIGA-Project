package cl.siga.coreshare.dto.asignatura.inscripcion;

import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoInscripcionRequestDTO(
    @NotNull (message = "El nuevo estado de la inscripción es obligatorio")
    EstadoInscripcion estado
) {}
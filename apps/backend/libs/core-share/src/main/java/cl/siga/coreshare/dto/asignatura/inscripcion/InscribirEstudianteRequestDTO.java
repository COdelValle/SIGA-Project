package cl.siga.coreshare.dto.asignatura.inscripcion;

import jakarta.validation.constraints.NotNull;

public record InscribirEstudianteRequestDTO(
    @NotNull (message = "El ID del estudiante es obligatorio")
    Long estudianteId
) {}
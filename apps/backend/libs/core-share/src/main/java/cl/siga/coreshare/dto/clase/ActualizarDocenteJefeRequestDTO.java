package cl.siga.coreshare.dto.clase;

import jakarta.validation.constraints.NotNull;

public record ActualizarDocenteJefeRequestDTO(
    @NotNull (message = "El ID del docente jefe es obligatorio")
    Long idDocenteJefe
) {}

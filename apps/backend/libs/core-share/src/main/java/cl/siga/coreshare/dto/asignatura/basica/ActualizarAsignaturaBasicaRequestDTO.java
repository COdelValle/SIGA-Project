package cl.siga.coreshare.dto.asignatura.basica;

import cl.siga.coreshare.enums.AreaAcademica;
import jakarta.validation.constraints.*;

public record ActualizarAsignaturaBasicaRequestDTO(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50)
    String name,

    @NotBlank(message = "La descripción es requerida")
    @Size(min = 2, max = 150)
    String description,

    @NotNull(message = "El área académica es obligatoria")
    AreaAcademica area,

    @NotNull(message = "El ID del docente es obligatorio")
    Long idDocente
) {}

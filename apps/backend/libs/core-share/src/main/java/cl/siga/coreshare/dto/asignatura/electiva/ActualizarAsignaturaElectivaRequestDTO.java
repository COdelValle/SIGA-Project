package cl.siga.coreshare.dto.asignatura.electiva;

import cl.siga.coreshare.enums.AreaAcademica;
import jakarta.validation.constraints.*;

public record ActualizarAsignaturaElectivaRequestDTO(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50)
    String name,

    @NotBlank(message = "La descripción es requerida")
    @Size(min = 2, max = 150)
    String description,

    @NotNull(message = "El área académica es obligatoria")
    AreaAcademica area,

    @NotNull(message = "El ID del docente es obligatorio")
    Long idDocente,

    @NotNull(message = "El cupo máximo es obligatorio para un electivo")
    @Min(value = 1, message = "El cupo máximo debe ser al menos 1")
    Integer cupoMaximo
) {}
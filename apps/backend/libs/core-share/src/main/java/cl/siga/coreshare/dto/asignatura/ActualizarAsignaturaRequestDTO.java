package cl.siga.coreshare.dto.asignatura;

import cl.siga.coreshare.enums.AreaAcademica;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarAsignaturaRequestDTO(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 80)
    String nombre,

    @Size(max = 40, message = "El nombre corto no puede superar los 40 caracteres")
    String nombreCorto,

    @NotBlank(message = "La descripción es requerida")
    @Size(min = 2, max = 150)
    String descripcion,

    @NotNull(message = "El área académica es obligatoria")
    AreaAcademica area,

    @NotNull(message = "Debe indicar si la asignatura es calificable")
    Boolean calificable
) {}

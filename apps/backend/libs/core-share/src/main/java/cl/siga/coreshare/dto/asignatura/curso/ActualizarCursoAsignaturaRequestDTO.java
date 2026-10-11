package cl.siga.coreshare.dto.asignatura.curso;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarCursoAsignaturaRequestDTO(
    @NotNull(message = "El ID del docente es obligatorio")
    Long idDocente,

    @NotNull(message = "El semestre es obligatorio")
    Semestre semestre,

    @NotNull(message = "El carácter de la asignatura es obligatorio")
    CaracterAsignatura caracter,

    @Min(value = 1, message = "El cupo máximo debe ser al menos 1")
    Integer cupoMaximo
) {}

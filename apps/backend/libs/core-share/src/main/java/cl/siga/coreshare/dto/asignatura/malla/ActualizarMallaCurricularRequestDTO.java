package cl.siga.coreshare.dto.asignatura.malla;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarMallaCurricularRequestDTO(
    @NotNull(message = "El carácter de la asignatura es obligatorio")
    CaracterAsignatura caracter,

    @NotNull(message = "El plan de formación es obligatorio")
    PlanFormacion plan,

    @Min(value = 1, message = "Las horas semanales deben ser al menos 1")
    Integer horasSemanales
) {}

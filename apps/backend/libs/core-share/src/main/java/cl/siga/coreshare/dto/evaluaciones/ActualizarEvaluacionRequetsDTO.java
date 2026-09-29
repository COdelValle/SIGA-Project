package cl.siga.coreshare.dto.evaluaciones;

import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarEvaluacionRequetsDTO(
  @NotNull(message = "El tipo es requerido")
  TipoEvaluacion tipo,

  @NotNull(message = "La ponderación es requerida")
  @Min(value = 0, message = "La ponderación debe ser mayor o igual a 0")
  @DecimalMax(value = "100.0", message = "La ponderación debe ser menor o igual a 100")
  Double ponderacion
) {
}

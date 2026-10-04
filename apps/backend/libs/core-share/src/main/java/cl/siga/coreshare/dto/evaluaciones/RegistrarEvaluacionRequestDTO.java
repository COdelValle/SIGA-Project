package cl.siga.coreshare.dto.evaluaciones;

import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import jakarta.validation.constraints.*;

public record RegistrarEvaluacionRequestDTO(
  @NotBlank(message = "El nombre es requerido")
  @Size(min = 2, max = 50)
  String nombre,

  @NotNull(message = "El tipo es requerido")
  TipoEvaluacion tipo,

  @NotNull(message = "La ponderación es requerida")
  @DecimalMin(value = "0.0", message = "La ponderación debe ser mayor o igual a 0")
  @DecimalMax(value = "100.0", message = "La ponderación debe ser menor o igual a 100")
  Double ponderacion,

  @NotNull(message = "El ID de la dictación es requerido")
  Long idCursoAsignatura
  ) {
}

package cl.siga.coreshare.dto.evaluaciones;

import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;

public record EvaluacionResponseDTO(
  Long id,
  String nombre,
  TipoEvaluacion tipo,
  Double ponderacion,
  Long idAsignatura,
  boolean active
) {
}

package cl.siga.coreshare.dto.evaluaciones.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoEvaluacion {
  @JsonProperty("FORMATIVA")
  FORMATIVA ("Evaluación Formativa"),

  @JsonProperty("DIAGNOSTICO")
  DIAGNOSTICO ("Evaluación Diagnóstica"),

  @JsonProperty("SUMATIVA")
  SUMATIVA ("Evaluación Sumativa");

  private final String nombre;

  TipoEvaluacion(String nombre) {
    this.nombre = nombre;
  }

  @JsonValue
  public String getNombre() {
    return nombre;
  }
}

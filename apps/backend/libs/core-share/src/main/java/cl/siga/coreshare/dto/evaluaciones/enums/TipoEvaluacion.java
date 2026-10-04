package cl.siga.coreshare.dto.evaluaciones.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

  /**
   * Acepta tanto el nombre de la constante (FORMATIVA) como el texto visible
   * (Evaluación Formativa), sin distinguir mayusculas, para que el frontend
   * pueda hacer round-trip del valor que recibe.
   */
  @JsonCreator
  public static TipoEvaluacion desde(String valor) {
    if (valor == null) {
      return null;
    }
    for (TipoEvaluacion tipo : values()) {
      if (tipo.name().equalsIgnoreCase(valor) || tipo.nombre.equalsIgnoreCase(valor)) {
        return tipo;
      }
    }
    throw new IllegalArgumentException("Tipo de evaluación inválido: " + valor);
  }
}

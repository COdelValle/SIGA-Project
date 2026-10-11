package cl.siga.coreshare.dto.evaluaciones.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

  // El JSON usa el nombre de la constante (FORMATIVA/DIAGNOSTICO/SUMATIVA):
  // el frontend tipa y compara contra esos valores. El texto visible se
  // conserva en getNombre() para mensajes de negocio.
  public String getNombre() {
    return nombre;
  }

  /**
   * Acepta tanto el nombre de la constante (FORMATIVA) como el texto visible
   * legacy (Evaluación Formativa), sin distinguir mayusculas, para tolerar
   * payloads antiguos del outbox y clientes previos al cambio de contrato.
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

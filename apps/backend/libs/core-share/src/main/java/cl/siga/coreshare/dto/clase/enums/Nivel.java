package cl.siga.coreshare.dto.clase.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Nivel {
    @JsonProperty("PRIMERO_BASICO")
    PRIMERO_BASICO("1ro Básico"),

    @JsonProperty("SEGUNDO_BASICO")
    SEGUNDO_BASICO("2do Básico"),

    @JsonProperty ("TERCERO_BASICO")
    TERCERO_BASICO("3ro Básico"),

    @JsonProperty("CUARTO_BASICO")
    CUARTO_BASICO("4to Básico"),

    @JsonProperty("QUINTO_BASICO")
    QUINTO_BASICO("5to Básico"),

    @JsonProperty("SEXTO_BASICO")
    SEXTO_BASICO("6to Básico"),

    @JsonProperty("SEPTIMO_BASICO")
    SEPTIMO_BASICO("7mo Básico"),

    @JsonProperty("OCTAVO_BASICO")
    OCTAVO_BASICO("8vo Básico"),
    
    @JsonProperty("PRIMERO_MEDIO")
    PRIMERO_MEDIO("1ro Medio"),

    @JsonProperty("SEGUNDO_MEDIO")
    SEGUNDO_MEDIO("2do Medio"),

    @JsonProperty("TERCERO_MEDIO")
    TERCERO_MEDIO("3ro Medio"),

    @JsonProperty("CUARTO_MEDIO")
    CUARTO_MEDIO("4to Medio");

    private final String descripcion;

    Nivel(String descripcion) {
        this.descripcion = descripcion;
    }

    @JsonValue
    public String getDescripcion() {
        return descripcion;
    }
}

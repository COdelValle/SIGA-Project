package cl.siga.coreshare.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AreaAcademica {
    @JsonProperty("MATEMATICAS")
    MATEMATICAS ("Matemáticas"),

    @JsonProperty("CIENCIAS")
    CIENCIAS ("Ciencias Naturales y Exactas"),

    @JsonProperty("CIENCIAS_CIUDADANIA")
    CIENCIAS_CIUDADANIA ("Ciencias para la Ciudadanía"),

    @JsonProperty("LENGUAJE")
    LENGUAJE ("Lenguaje y Comunicación"),

    @JsonProperty("HISTORIA")
    HISTORIA ("Historia y Ciencias Sociales"),

    @JsonProperty("CIUDADANIA")
    CIUDADANIA ("Formación Ciudadana"),

    @JsonProperty("FILOSOFIA")
    FILOSOFIA ("Filosofía"),

    @JsonProperty("IDIOMAS")
    IDIOMAS ("Lenguas e Idiomas"),

    @JsonProperty("ARTES")
    ARTES ("Artes y Música"),

    @JsonProperty("EDUCACION_FISICA")
    EDUCACION_FISICA ("Educación Física"),

    @JsonProperty("TECNOLOGIA")
    TECNOLOGIA ("Tecnología e Informática"),

    @JsonProperty("ORIENTACION")
    ORIENTACION ("Orientación"),

    @JsonProperty("RELIGION")
    RELIGION ("Religión"),

    @JsonProperty("ECONOMIA")
    ECONOMIA ("Economía y Finanzas"),

    @JsonProperty("OTRA")
    OTRA ("Otra Área");

    private final String nombre;

    AreaAcademica(String nombre) {
        this.nombre = nombre;
    }

    @JsonValue
    public String getNombre() {
        return nombre;
    }
}

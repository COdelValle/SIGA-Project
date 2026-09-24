package cl.siga.coreshare.dto.docente.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AreaAcademica {
    @JsonProperty("MATEMATICAS")
    MATEMATICAS ("Matemáticas"),

    @JsonProperty("CIENCIAS")
    CIENCIAS ("Ciencias Naturales y Exactas"),

    @JsonProperty("LENGUAJE")
    LENGUAJE ("Lenguaje y Comunicación"),

    @JsonProperty("HISTORIA")
    HISTORIA ("Historia y Ciencias Sociales"),

    @JsonProperty("IDIOMAS")
    IDIOMAS ("Idiomas Extranjeros"),

    @JsonProperty("ARTES")
    ARTES ("Artes y Música"),

    @JsonProperty("EDUCACION_FISICA")
    EDUCACION_FISICA ("Educación Física"),

    @JsonProperty("TECNOLOGIA")
    TECNOLOGIA ("Tecnología e Informática"),

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

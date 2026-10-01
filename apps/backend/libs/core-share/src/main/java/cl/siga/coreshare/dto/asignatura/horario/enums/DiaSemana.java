package cl.siga.coreshare.dto.asignatura.horario.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DiaSemana {
    @JsonProperty ("LUNES")
    LUNES("Lunes"), 

    @JsonProperty("MARTES")
    MARTES("Martes"), 

    @JsonProperty("MIERCOLES")
    MIERCOLES("Miércoles"), 

    @JsonProperty("JUEVES")
    JUEVES("Jueves"), 
    
    @JsonProperty("VIERNES")
    VIERNES("Viernes");

    private final String nombre;

    DiaSemana(String nombre) {
        this.nombre = nombre;
    }

    @JsonValue 
    public String getNombre() {
        return nombre;
    }
}

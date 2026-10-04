package cl.siga.coreshare.dto.asignatura.horario.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    /**
     * Acepta tanto el nombre de la constante (LUNES) como el texto visible
     * (Lunes), sin distinguir mayusculas.
     */
    @JsonCreator
    public static DiaSemana desde(String valor) {
        if (valor == null) {
            return null;
        }
        for (DiaSemana dia : values()) {
            if (dia.name().equalsIgnoreCase(valor) || dia.nombre.equalsIgnoreCase(valor)) {
                return dia;
            }
        }
        throw new IllegalArgumentException("Día de la semana inválido: " + valor);
    }
}

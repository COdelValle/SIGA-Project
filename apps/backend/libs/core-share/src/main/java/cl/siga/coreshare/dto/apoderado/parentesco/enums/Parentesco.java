package cl.siga.coreshare.dto.apoderado.parentesco.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Parentesco {
    @JsonProperty("MADRE_PADRE")
    MADRE_PADRE("Madre/Padre"),
    
    @JsonProperty("TIA_TIO")
    TIA_TIO("Tía/Tío"),
    
    @JsonProperty("ABUELA_ABUELO")
    ABUELA_ABUELO("Abuela/Abuelo"),
    
    @JsonProperty("HERMANA_HERMANO")
    HERMANA_HERMANO("Hermana/Hermano"),
    
    @JsonProperty("PRIMO_PRIMA")
    PRIMO_PRIMA("Primo/Prima"),
    
    @JsonProperty("TUTOR_LEGAL")
    TUTOR_LEGAL("Tutor/a Legal"),

    @JsonProperty ("OTRO")
    OTRO("Otro");
 
    private final String textoMostrado;
 
    Parentesco(String textoMostrado) {
        this.textoMostrado = textoMostrado;
    }
 
    @JsonValue
    public String getTextoMostrado() {
        return this.textoMostrado;
    }
}

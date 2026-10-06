package cl.siga.coreshare.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NombrePropioTest {

    @Test
    void corrigeMayusculasDesordenadas() {
        assertEquals("Catalina", NombrePropio.normalizar("CAtAlina"));
        assertEquals("Catalina Ormeño Soto", NombrePropio.normalizar("CATALINA ORMEÑO SOTO"));
    }

    @Test
    void respetaParticulasEnMinuscula() {
        assertEquals("Juan de la Rosa", NombrePropio.normalizar("JUAN DE LA ROSA"));
        assertEquals("Maria del Carmen de los Angeles",
                NombrePropio.normalizar("MARIA DEL CARMEN DE LOS ANGELES"));
        assertEquals("del Valle", NombrePropio.normalizar("DEL VALLE"));
        assertEquals("de la Fuente", NombrePropio.normalizar("DE LA FUENTE"));
    }

    @Test
    void aplicaPrefijosMcYMac() {
        assertEquals("McDonald", NombrePropio.normalizar("MCDONALD"));
        assertEquals("MacLean", NombrePropio.normalizar("maclean"));
        assertEquals("Macarena", NombrePropio.normalizar("MACARENA"));
    }

    @Test
    void respetaApostrofes() {
        assertEquals("O'Hara", NombrePropio.normalizar("O'HARA"));
        assertEquals("D'Angelo", NombrePropio.normalizar("d'angelo"));
    }

    @Test
    void capitalizaSegmentosConGuion() {
        assertEquals("Pérez-Gómez", NombrePropio.normalizar("PÉREZ-GÓMEZ"));
        assertEquals("Perez-Gomez", NombrePropio.normalizar("PEREZ-GOMEZ"));
        assertEquals("María-José", NombrePropio.normalizar("maría-josé"));
    }

    @Test
    void colapsaEspacios() {
        assertEquals("Ana Maria", NombrePropio.normalizar("  ana   maria  "));
    }

    @Test
    void soportaNullYVacio() {
        assertNull(NombrePropio.normalizar(null));
        assertEquals("", NombrePropio.normalizar("   "));
    }
}

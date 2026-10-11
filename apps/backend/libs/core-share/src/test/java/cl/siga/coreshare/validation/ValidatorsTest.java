package cl.siga.coreshare.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidatorsTest {

    private final RUTValidator rut = new RUTValidator();
    private final PhoneValidator phone = new PhoneValidator();
    private final ChileanGradeValidator grade = new ChileanGradeValidator();

    @Test
    void rutValidoConFormato() {
        assertTrue(rut.isValid("12.345.678-5", null));
    }

    @Test
    void rutConDigitoInvalido() {
        assertFalse(rut.isValid("12.345.678-9", null));
    }

    @Test
    void rutNuloEsInvalido() {
        assertFalse(rut.isValid(null, null));
    }

    @Test
    void telefonoValidoConPrefijo() {
        assertTrue(phone.isValid("+56912345678", null));
    }

    @Test
    void telefonoNuloEsValido() {
        assertTrue(phone.isValid(null, null));
    }

    @Test
    void telefonoConLetrasEsInvalido() {
        assertFalse(phone.isValid("+56abc", null));
    }

    @Test
    void notaEnRango() {
        assertTrue(grade.isValid(6.5, null));
    }

    @Test
    void notaFueraDeRango() {
        assertFalse(grade.isValid(8.0, null));
    }
}

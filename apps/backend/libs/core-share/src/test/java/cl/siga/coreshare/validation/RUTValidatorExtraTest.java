package cl.siga.coreshare.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RUTValidatorExtraTest {

    private final RUTValidator validator = new RUTValidator();

    @Test
    void initializeNoRequiereConfiguracion() {
        validator.initialize(null);
        assertThat(validator).isNotNull();
    }

    @Test
    void rechazaRutCortoYConLetrasEnElCuerpo() {
        assertThat(validator.isValid("123", null)).isFalse();
        assertThat(validator.isValid("12.345.67A-5", null)).isFalse();
        assertThat(validator.isValid("ABCDEFGH-1", null)).isFalse();
    }

    @Test
    void rechazaCuerpoNumericoFueraDelRangoEntero() {
        assertThat(validator.isValid("99999999999-1", null)).isFalse();
    }

}

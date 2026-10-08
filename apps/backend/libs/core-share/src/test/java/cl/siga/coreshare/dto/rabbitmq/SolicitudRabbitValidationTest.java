package cl.siga.coreshare.dto.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class SolicitudRabbitValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void colaSinNombreEsInvalida() {
        assertThat(validator.validate(new SolicitudColaDTO("", null, null))).isNotEmpty();
        assertThat(validator.validate(new SolicitudColaDTO("   ", null, null))).isNotEmpty();
    }

    @Test
    void colaConCaracteresInvalidosEsRechazada() {
        assertThat(validator.validate(new SolicitudColaDTO("cola con espacios", null, null))).isNotEmpty();
        assertThat(validator.validate(new SolicitudColaDTO("cola;drop", null, null))).isNotEmpty();
    }

    @Test
    void colaConDeadLetterOpcionalEsValida() {
        assertThat(validator.validate(new SolicitudColaDTO("cola.prueba", null, null))).isEmpty();
        assertThat(validator.validate(new SolicitudColaDTO(
            "cola.prueba", "siga.dlx.direct", "cola.prueba.dlq"))).isEmpty();
    }

    @Test
    void exchangeSinTipoEsInvalido() {
        assertThat(validator.validate(new SolicitudExchangeDTO("siga.exchange.prueba", null))).isNotEmpty();
        assertThat(validator.validate(new SolicitudExchangeDTO(
            "siga.exchange.prueba", TipoExchange.DIRECT))).isEmpty();
    }

    @Test
    void bindingRequiereColaYExchange() {
        assertThat(validator.validate(new SolicitudBindingDTO(null, "siga.exchange", "ruta")))
            .isNotEmpty();
        assertThat(validator.validate(new SolicitudBindingDTO("cola", null, "ruta")))
            .isNotEmpty();
        assertThat(validator.validate(new SolicitudBindingDTO(
            "cola.prueba", "siga.exchange", "ruta.*"))).isEmpty();
    }

    @Test
    void routingKeySinTextoEsValidaParaFanout() {
        assertThat(validator.validate(new SolicitudBindingDTO("cola.prueba", "siga.exchange", null)))
            .isEmpty();
        assertThat(validator.validate(new SolicitudBindingDTO("cola.prueba", "siga.exchange", "")))
            .isEmpty();
    }

    @Test
    void routingKeyConCaracteresInvalidosEsRechazada() {
        assertThat(validator.validate(new SolicitudBindingDTO(
            "cola.prueba", "siga.exchange", "ruta con espacios"))).isNotEmpty();
    }
}

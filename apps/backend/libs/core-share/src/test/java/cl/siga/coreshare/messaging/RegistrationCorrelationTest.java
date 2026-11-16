package cl.siga.coreshare.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import cl.siga.coreshare.config.CorrelationIdConfig;

class RegistrationCorrelationTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void publicaElCorrelationIdEnElMdcYLimpiaAlTerminar() {
        AtomicReference<String> visto = new AtomicReference<>();

        RegistrationCorrelation.ejecutar("corr-1", () -> visto.set(MDC.get(CorrelationIdConfig.MDC_KEY)));

        assertThat(visto.get()).isEqualTo("corr-1");
        assertThat(MDC.get(CorrelationIdConfig.MDC_KEY)).isNull();
    }

    @Test
    void sinCorrelationIdNoPublicaNadaEnElMdc() {
        AtomicReference<String> visto = new AtomicReference<>("inicial");

        RegistrationCorrelation.ejecutar(null, () -> visto.set(MDC.get(CorrelationIdConfig.MDC_KEY)));

        assertThat(visto.get()).isNull();
    }

    @Test
    void correlationIdEnBlancoNoSePublica() {
        AtomicReference<String> visto = new AtomicReference<>("inicial");

        RegistrationCorrelation.ejecutar("   ", () -> visto.set(MDC.get(CorrelationIdConfig.MDC_KEY)));

        assertThat(visto.get()).isNull();
    }

    @Test
    void limpiaElMdcAunqueLaAccionFalle() {
        assertThatThrownBy(() -> RegistrationCorrelation.ejecutar("corr-2", () -> {
            throw new IllegalStateException("fallo");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(CorrelationIdConfig.MDC_KEY)).isNull();
    }
}

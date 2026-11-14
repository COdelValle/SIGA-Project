package cl.siga.msnotas.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;

class EvaluacionClientFallbackFactoryTest {

    @Test
    void respondeNoDisponibleAlVerificarExistencia() {
        EvaluacionClient client = new EvaluacionClientFallbackFactory()
            .create(new RuntimeException("sin conexión"));

        assertThatThrownBy(() -> client.existsById(9L))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("No se pudo verificar la existencia de la evaluación 9")
            .hasMessageContaining("HTTP -1");
    }

    @Test
    void respondeNoDisponibleAlObtenerLaEvaluacion() {
        FeignException causa = mock(FeignException.class);
        when(causa.status()).thenReturn(502);
        EvaluacionClient client = new EvaluacionClientFallbackFactory().create(causa);

        assertThatThrownBy(() -> client.getEvaluacionById(9L))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("No se pudo obtener la evaluación 9")
            .hasMessageContaining("HTTP 502");
    }
}

package cl.siga.msevaluaciones.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;

class AsignaturaClientFallbackFactoryTest {

    @Test
    void respondeNoDisponibleCuandoLaCausaNoEsFeign() {
        AsignaturaClient client = new AsignaturaClientFallbackFactory()
            .create(new RuntimeException("sin conexión"));

        assertThatThrownBy(() -> client.getCursoAsignaturaById(5L))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("No se pudo obtener la dictación 5")
            .hasMessageContaining("HTTP -1");
    }

    @Test
    void incluyeElStatusHttpDeLaCausaFeign() {
        FeignException causa = mock(FeignException.class);
        when(causa.status()).thenReturn(503);
        AsignaturaClient client = new AsignaturaClientFallbackFactory().create(causa);

        assertThatThrownBy(() -> client.getCursoAsignaturaById(5L))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("HTTP 503");
    }
}

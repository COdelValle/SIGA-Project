package cl.siga.msasistencias.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;

class EstudianteClientFallbackFactoryTest {

    @Test
    void respondeNoDisponibleCuandoLaCausaNoEsFeign() {
        EstudianteClient client = new EstudianteClientFallbackFactory()
            .create(new RuntimeException("sin conexión"));

        assertThatThrownBy(() -> client.existsById(1L))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("No se pudo verificar la existencia del estudiante 1")
            .hasMessageContaining("HTTP -1");
    }

    @Test
    void incluyeElStatusHttpDeLaCausaFeign() {
        FeignException causa = mock(FeignException.class);
        when(causa.status()).thenReturn(500);
        EstudianteClient client = new EstudianteClientFallbackFactory().create(causa);

        assertThatThrownBy(() -> client.existsById(1L))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("HTTP 500");
    }
}

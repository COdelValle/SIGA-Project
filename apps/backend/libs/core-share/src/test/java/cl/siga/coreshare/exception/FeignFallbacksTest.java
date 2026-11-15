package cl.siga.coreshare.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import feign.FeignException;

class FeignFallbacksTest {

    @Test
    void incluyeElStatusRealDeLaCausaFeign() {
        FeignException causa = mock(FeignException.class);
        when(causa.status()).thenReturn(503);
        when(causa.getMessage()).thenReturn("Service Unavailable");

        ServiceUnavailableException error = FeignFallbacks.noDisponible(causa, "ms-estudiantes no disponible");

        assertThat(error.getMessage())
                .isEqualTo("ms-estudiantes no disponible (HTTP 503)");
    }

    @Test
    void usaMenosUnoCuandoLaCausaNoEsFeign() {
        ServiceUnavailableException error = FeignFallbacks.noDisponible(
                new IllegalStateException("conexion rechazada"), "ms-notas no disponible");

        assertThat(error.getMessage())
                .isEqualTo("ms-notas no disponible (HTTP -1)");
    }

    @Test
    void lasExcepcionesConservanMensajeYCausa() {
        IllegalStateException causa = new IllegalStateException("causa");

        assertThat(new BadRequestException("solicitud").getMessage()).isEqualTo("solicitud");
        assertThat(new BusinessException("regla", causa).getCause()).isSameAs(causa);
        assertThat(new BusinessException("regla").getMessage()).isEqualTo("regla");
        assertThat(new ConflictException("conflicto").getMessage()).isEqualTo("conflicto");
        assertThat(new ResourceNotFoundException("sin recurso").getMessage()).isEqualTo("sin recurso");
        assertThat(new ServiceUnavailableException("caido").getMessage()).isEqualTo("caido");
    }
}

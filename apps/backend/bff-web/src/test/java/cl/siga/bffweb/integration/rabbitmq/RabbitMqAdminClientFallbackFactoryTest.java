package cl.siga.bffweb.integration.rabbitmq;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.bffweb.integration.FeignErrorTranslator;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import feign.FeignException;

class RabbitMqAdminClientFallbackFactoryTest {

    @Test
    void traduce404DelAdministradorComoResourceNotFoundYNoComo503() {
        FeignException causa = mock(FeignException.class);
        when(causa.status()).thenReturn(404);
        when(causa.contentUTF8()).thenReturn("{\"message\":\"La cola no existe.\"}");
        RabbitMqAdminClientFallbackFactory factory = new RabbitMqAdminClientFallbackFactory(
            new FeignErrorTranslator(new ObjectMapper()));

        RabbitMqAdminClient client = factory.create(causa);

        assertThatThrownBy(() -> client.estadoCola("perf.inexistente"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("La cola no existe.");
    }
}

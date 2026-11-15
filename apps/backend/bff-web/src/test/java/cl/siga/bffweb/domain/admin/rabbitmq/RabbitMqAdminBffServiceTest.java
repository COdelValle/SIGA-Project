package cl.siga.bffweb.domain.admin.rabbitmq;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import cl.siga.bffweb.integration.rabbitmq.RabbitMqAdminClient;
import cl.siga.coreshare.dto.rabbitmq.EstadoColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudBindingDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudExchangeDTO;

class RabbitMqAdminBffServiceTest {

    private final RabbitMqAdminClient client = mock(RabbitMqAdminClient.class);
    private final RabbitMqAdminBffService service = new RabbitMqAdminBffService(client);

    @Test
    void declararColaDelegaEnElCliente() {
        SolicitudColaDTO solicitud = new SolicitudColaDTO("cola.test", null, null);

        service.declararCola(solicitud);

        verify(client).declararCola(solicitud);
    }

    @Test
    void estadoColaDevuelveElEstadoDelCliente() {
        EstadoColaDTO estado = new EstadoColaDTO("cola.test", 3, 1);
        when(client.estadoCola("cola.test")).thenReturn(estado);

        var resultado = service.estadoCola("cola.test");

        org.assertj.core.api.Assertions.assertThat(resultado).isEqualTo(estado);
        verify(client).estadoCola("cola.test");
    }

    @Test
    void eliminarColaDelegaEnElCliente() {
        service.eliminarCola("cola.test");

        verify(client).eliminarCola("cola.test");
    }

    @Test
    void declararYELiminarExchangeDeleganEnElCliente() {
        SolicitudExchangeDTO solicitud = mock(SolicitudExchangeDTO.class);

        service.declararExchange(solicitud);
        service.eliminarExchange("exchange.test");

        verify(client).declararExchange(solicitud);
        verify(client).eliminarExchange("exchange.test");
    }

    @Test
    void declararYELiminarBindingDeleganEnElCliente() {
        SolicitudBindingDTO solicitud = new SolicitudBindingDTO("cola.test", "exchange.test", "rk");

        service.declararBinding(solicitud);
        service.eliminarBinding("cola.test", "exchange.test", "rk");

        verify(client).declararBinding(solicitud);
        verify(client).eliminarBinding("cola.test", "exchange.test", "rk");
    }
}

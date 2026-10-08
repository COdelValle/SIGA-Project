package cl.siga.msrabbitmqadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import cl.siga.coreshare.dto.rabbitmq.EstadoColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudBindingDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudExchangeDTO;
import cl.siga.coreshare.dto.rabbitmq.TipoExchange;
import cl.siga.coreshare.exception.BadRequestException;
import cl.siga.coreshare.exception.ResourceNotFoundException;

class RabbitAdminServiceTest {

    private AmqpAdmin amqpAdmin;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        amqpAdmin = mock(AmqpAdmin.class);
        service = new RabbitAdminService(amqpAdmin);
    }

    @Test
    void declaraColaDurableConDeadLetter() {
        service.declararCola(new SolicitudColaDTO("cola.prueba", "siga.dlx.direct", "cola.prueba.dlq"));

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        Queue cola = captor.getValue();
        assertThat(cola.getName()).isEqualTo("cola.prueba");
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
            .containsEntry("x-dead-letter-exchange", "siga.dlx.direct")
            .containsEntry("x-dead-letter-routing-key", "cola.prueba.dlq");
    }

    @Test
    void estadoColaTraduceLasPropiedadesDelBroker() {
        Properties propiedades = new Properties();
        propiedades.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 5);
        propiedades.put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 2);
        when(amqpAdmin.getQueueProperties("cola.prueba")).thenReturn(propiedades);

        EstadoColaDTO estado = service.estadoCola("cola.prueba");

        assertThat(estado.nombre()).isEqualTo("cola.prueba");
        assertThat(estado.mensajes()).isEqualTo(5);
        assertThat(estado.consumidores()).isEqualTo(2);
    }

    @Test
    void estadoColaInexistenteRespondeNoEncontrada() {
        when(amqpAdmin.getQueueProperties("fantasma")).thenReturn(null);

        assertThatThrownBy(() -> service.estadoCola("fantasma"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminarColaInexistenteRespondeNoEncontrada() {
        when(amqpAdmin.getQueueProperties("fantasma")).thenReturn(null);

        assertThatThrownBy(() -> service.eliminarCola("fantasma"))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminaColaExistente() {
        when(amqpAdmin.getQueueProperties("cola.prueba")).thenReturn(new Properties());

        service.eliminarCola("cola.prueba");

        verify(amqpAdmin).deleteQueue("cola.prueba");
    }

    @Test
    void declaraExchangeSegunElTipo() {
        service.declararExchange(new SolicitudExchangeDTO("siga.exchange.prueba", TipoExchange.TOPIC));

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TopicExchange.class);
        assertThat(captor.getValue().getName()).isEqualTo("siga.exchange.prueba");
        assertThat(captor.getValue().isDurable()).isTrue();
    }

    @Test
    void declararExchangeRechazaElErrorDelBrokerComoBadRequest() {
        doThrow(new AmqpException("PRECONDITION_FAILED")).when(amqpAdmin).declareExchange(any(Exchange.class));

        assertThatThrownBy(() -> service.declararExchange(
            new SolicitudExchangeDTO("siga.exchange.prueba", TipoExchange.DIRECT)))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void declaraBindingEntreColaYExchange() {
        service.declararBinding(new SolicitudBindingDTO("cola.prueba", "siga.exchange.prueba", "ruta.*"));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(captor.capture());
        Binding binding = captor.getValue();
        assertThat(binding.getDestination()).isEqualTo("cola.prueba");
        assertThat(binding.getDestinationType()).isEqualTo(Binding.DestinationType.QUEUE);
        assertThat(binding.getExchange()).isEqualTo("siga.exchange.prueba");
        assertThat(binding.getRoutingKey()).isEqualTo("ruta.*");
    }

    @Test
    void declararBindingFallaDeNegocioSiElBrokerRechaza() {
        doThrow(new AmqpException("NOT_FOUND")).when(amqpAdmin).declareBinding(any(Binding.class));

        assertThatThrownBy(() -> service.declararBinding(
            new SolicitudBindingDTO("cola.prueba", "siga.exchange.prueba", "")))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void eliminaBindingEntreColaYExchange() {
        service.eliminarBinding(new SolicitudBindingDTO("cola.prueba", "siga.exchange.prueba", "ruta"));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).removeBinding(captor.capture());
        assertThat(captor.getValue().getExchange()).isEqualTo("siga.exchange.prueba");
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("ruta");
    }
}

package cl.siga.msrabbitmqadmin.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import cl.siga.coreshare.dto.rabbitmq.EstadoColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudBindingDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudExchangeDTO;
import cl.siga.coreshare.exception.BadRequestException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Encapsula la administración del broker con {@link AmqpAdmin}: declara y
 * elimina colas, exchanges y bindings, y expone el estado operativo de una
 * cola. La topología de negocio sigue declarándose por código (core-share);
 * esta API permite operar sobre el broker en caliente para pruebas y soporte.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private static final String ARG_DLX = "x-dead-letter-exchange";
    private static final String ARG_DLQ_KEY = "x-dead-letter-routing-key";

    private final AmqpAdmin amqpAdmin;

    /** Declara una cola durable con dead-letter opcional. */
    public void declararCola(SolicitudColaDTO solicitud) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        if (tieneTexto(solicitud.deadLetterExchange())) {
            argumentos.put(ARG_DLX, solicitud.deadLetterExchange().trim());
        }
        if (tieneTexto(solicitud.deadLetterRoutingKey())) {
            argumentos.put(ARG_DLQ_KEY, solicitud.deadLetterRoutingKey().trim());
        }
        Queue cola = QueueBuilder.durable(solicitud.nombre().trim()).withArguments(argumentos).build();
        amqpAdmin.declareQueue(cola);
        log.info("Cola declarada nombre={} argumentos={}", cola.getName(), argumentos);
    }

    /** Estado operativo de una cola: mensajes encolados y consumidores. */
    public EstadoColaDTO estadoCola(String nombre) {
        Properties propiedades = amqpAdmin.getQueueProperties(nombre);
        if (propiedades == null) {
            throw new ResourceNotFoundException("La cola " + nombre + " no existe.");
        }
        return new EstadoColaDTO(
            nombre,
            (Integer) propiedades.get(RabbitAdmin.QUEUE_MESSAGE_COUNT),
            (Integer) propiedades.get(RabbitAdmin.QUEUE_CONSUMER_COUNT));
    }

    /** Elimina una cola; si no existe responde 404. */
    public void eliminarCola(String nombre) {
        if (amqpAdmin.getQueueProperties(nombre) == null) {
            throw new ResourceNotFoundException("La cola " + nombre + " no existe.");
        }
        amqpAdmin.deleteQueue(nombre);
        log.info("Cola eliminada nombre={}", nombre);
    }

    /** Declara un exchange durable del tipo solicitado. */
    public void declararExchange(SolicitudExchangeDTO solicitud) {
        String nombre = solicitud.nombre().trim();
        Exchange exchange = switch (solicitud.tipo()) {
            case DIRECT -> ExchangeBuilder.directExchange(nombre).durable(true).build();
            case TOPIC -> ExchangeBuilder.topicExchange(nombre).durable(true).build();
            case FANOUT -> ExchangeBuilder.fanoutExchange(nombre).durable(true).build();
            case HEADERS -> ExchangeBuilder.headersExchange(nombre).durable(true).build();
        };
        try {
            amqpAdmin.declareExchange(exchange);
        } catch (AmqpException error) {
            throw new BadRequestException(
                "No se pudo declarar el exchange " + nombre + ": " + error.getMessage());
        }
        log.info("Exchange declarado nombre={} tipo={}", nombre, solicitud.tipo());
    }

    /** Elimina un exchange (idempotente en el broker). */
    public void eliminarExchange(String nombre) {
        amqpAdmin.deleteExchange(nombre);
        log.info("Exchange eliminado nombre={}", nombre);
    }

    /** Declara un binding cola-exchange. */
    public void declararBinding(SolicitudBindingDTO solicitud) {
        Binding binding = aBinding(solicitud);
        try {
            amqpAdmin.declareBinding(binding);
        } catch (AmqpException error) {
            throw new BadRequestException(
                "No se pudo crear el binding: verifica que la cola y el exchange existan.");
        }
        log.info("Binding declarado cola={} exchange={} routingKey={}",
            solicitud.cola(), solicitud.exchange(), clave(binding));
    }

    /** Elimina un binding cola-exchange. */
    public void eliminarBinding(SolicitudBindingDTO solicitud) {
        Binding binding = aBinding(solicitud);
        try {
            amqpAdmin.removeBinding(binding);
        } catch (AmqpException error) {
            throw new BadRequestException(
                "No se pudo eliminar el binding: verifica que la cola y el exchange existan.");
        }
        log.info("Binding eliminado cola={} exchange={} routingKey={}",
            solicitud.cola(), solicitud.exchange(), clave(binding));
    }

    private Binding aBinding(SolicitudBindingDTO solicitud) {
        return new Binding(
            solicitud.cola().trim(),
            Binding.DestinationType.QUEUE,
            solicitud.exchange().trim(),
            solicitud.routingKey() == null ? "" : solicitud.routingKey().trim(),
            null);
    }

    private static String clave(Binding binding) {
        return binding.getRoutingKey() == null ? "" : binding.getRoutingKey();
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}

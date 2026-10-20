package cl.siga.msrabbitmqadmin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import cl.siga.coreshare.dto.rabbitmq.SolicitudBindingDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudExchangeDTO;
import cl.siga.coreshare.dto.rabbitmq.TipoExchange;

/** CRUD de topología contra un broker real y desechable. */
@Testcontainers(disabledWithoutDocker = true)
class RabbitAdminServiceRabbitTest {

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer(
        DockerImageName.parse("rabbitmq:4.3.6-management-alpine"));

    @Test
    void administraColaExchangeBindingYEstadoContraRabbitMQ() {
        var connection = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
        connection.setUsername(RABBIT.getAdminUsername());
        connection.setPassword(RABBIT.getAdminPassword());
        var service = new RabbitAdminService(new RabbitAdmin(connection));
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String topic = "it.topic." + suffix;
        String dlx = "it.dlx." + suffix;
        String queue = "it.queue." + suffix;
        String dlq = "it.dlq." + suffix;
        String routingKey = "event.*";

        try {
            service.declararExchange(new SolicitudExchangeDTO(topic, TipoExchange.TOPIC));
            service.declararExchange(new SolicitudExchangeDTO(dlx, TipoExchange.DIRECT));
            service.declararCola(new SolicitudColaDTO(queue, dlx, dlq));
            service.declararCola(new SolicitudColaDTO(dlq, null, null));
            service.declararBinding(new SolicitudBindingDTO(queue, topic, routingKey));
            service.declararBinding(new SolicitudBindingDTO(dlq, dlx, dlq));

            var estado = service.estadoCola(queue);
            assertThat(estado.nombre()).isEqualTo(queue);
            assertThat(estado.mensajes()).isZero();
            assertThat(estado.consumidores()).isZero();

            service.eliminarBinding(new SolicitudBindingDTO(queue, topic, routingKey));
            service.eliminarBinding(new SolicitudBindingDTO(dlq, dlx, dlq));
            service.eliminarCola(queue);
            service.eliminarCola(dlq);
            service.eliminarExchange(topic);
            service.eliminarExchange(dlx);
        } finally {
            connection.destroy();
        }
    }
}

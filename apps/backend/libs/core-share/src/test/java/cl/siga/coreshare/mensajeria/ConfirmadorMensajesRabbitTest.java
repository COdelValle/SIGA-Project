package cl.siga.coreshare.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Prueba que ACK/NACK manual se refleja en RabbitMQ y enruta poison messages a DLQ. */
@Testcontainers(disabledWithoutDocker = true)
class ConfirmadorMensajesRabbitTest {

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer(
        DockerImageName.parse("rabbitmq:4.3.6-management-alpine"));

    @Test
    void ackDrenaColaYNackSinRequeueEnviaElMensajeALaDlq() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String queueName = "it.ack." + suffix;
        String dlqName = "it.dlq." + suffix;
        String dlxName = "it.dlx." + suffix;
        CachingConnectionFactory factory = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
        factory.setUsername(RABBIT.getAdminUsername());
        factory.setPassword(RABBIT.getAdminPassword());
        RabbitAdmin admin = new RabbitAdmin(factory);
        RabbitTemplate publisher = new RabbitTemplate(factory);
        RetryTemplate retries = new RetryTemplate();
        Map<Class<? extends Throwable>, Boolean> retryable = new HashMap<>();
        retryable.put(EventoInvalidoException.class, false);
        retries.setRetryPolicy(new SimpleRetryPolicy(3, retryable, true, true));
        retries.setBackOffPolicy(new NoBackOffPolicy());
        ConfirmadorMensajes confirmador = new ConfirmadorMensajes(retries);

        DirectExchange deadLetterExchange = new DirectExchange(dlxName, true, false);
        Queue mainQueue = QueueBuilder.durable(queueName)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
        Queue deadLetterQueue = QueueBuilder.durable(dlqName).build();
        admin.declareExchange(deadLetterExchange);
        admin.declareQueue(mainQueue);
        admin.declareQueue(deadLetterQueue);
        admin.declareBinding(BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(dlqName));

        try (Connection connection = factory.createConnection(); Channel channel = connection.createChannel(false)) {
            publisher.convertAndSend("", queueName, "evento-valido");
            GetResponse valid = esperarMensaje(channel, queueName);
            assertThat(valid).isNotNull();
            confirmador.procesar(channel, valid.getEnvelope().getDeliveryTag(), queueName, () -> {});
            esperarConteo(admin, queueName, 0);
            esperarConteo(admin, dlqName, 0);

            publisher.convertAndSend("", queueName, "evento-invalido");
            GetResponse invalid = esperarMensaje(channel, queueName);
            assertThat(invalid).isNotNull();
            confirmador.procesar(channel, invalid.getEnvelope().getDeliveryTag(), queueName,
                () -> { throw new EventoInvalidoException("payload inválido sintético"); });
            esperarConteo(admin, queueName, 0);
            esperarConteo(admin, dlqName, 1);

            admin.purgeQueue(dlqName);
        } finally {
            admin.deleteQueue(queueName);
            admin.deleteQueue(dlqName);
            admin.deleteExchange(dlxName);
            factory.destroy();
        }
    }

    private static GetResponse esperarMensaje(Channel channel, String cola) throws Exception {
        long limite = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < limite) {
            GetResponse respuesta = channel.basicGet(cola, false);
            if (respuesta != null) {
                return respuesta;
            }
            Thread.sleep(50);
        }
        return null;
    }

    private static void esperarConteo(RabbitAdmin admin, String cola, int esperado) throws Exception {
        long limite = System.currentTimeMillis() + 5_000;
        Integer ultimo = null;
        while (System.currentTimeMillis() < limite) {
            var propiedades = admin.getQueueProperties(cola);
            Object valor = propiedades == null ? null : propiedades.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
            ultimo = valor == null ? null : ((Number) valor).intValue();
            if (ultimo != null && ultimo == esperado) {
                return;
            }
            Thread.sleep(50);
        }
        assertThat(ultimo).isEqualTo(esperado);
    }
}

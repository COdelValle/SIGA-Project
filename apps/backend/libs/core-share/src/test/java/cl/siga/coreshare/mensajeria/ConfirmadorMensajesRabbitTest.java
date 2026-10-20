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
            GetResponse valid = channel.basicGet(queueName, false);
            assertThat(valid).isNotNull();
            confirmador.procesar(channel, valid.getEnvelope().getDeliveryTag(), queueName, () -> {});
            assertThat(admin.getQueueProperties(queueName).get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).isEqualTo(0);
            assertThat(admin.getQueueProperties(dlqName).get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).isEqualTo(0);

            publisher.convertAndSend("", queueName, "evento-invalido");
            GetResponse invalid = channel.basicGet(queueName, false);
            assertThat(invalid).isNotNull();
            confirmador.procesar(channel, invalid.getEnvelope().getDeliveryTag(), queueName,
                () -> { throw new EventoInvalidoException("payload inválido sintético"); });
            assertThat(admin.getQueueProperties(queueName).get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).isEqualTo(0);
            assertThat(admin.getQueueProperties(dlqName).get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).isEqualTo(1);

            admin.purgeQueue(dlqName);
        } finally {
            admin.deleteQueue(queueName);
            admin.deleteQueue(dlqName);
            admin.deleteExchange(dlxName);
            factory.destroy();
        }
    }
}

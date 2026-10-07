package cl.siga.coreshare.messaging;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Topología compartida del registro asíncrono. Se activa solo en los servicios
 * que incluyen AMQP en el classpath (ms-usuarios-auth, ms-estudiantes,
 * ms-docentes y ms-apoderados) para que todos declaren las mismas colas y
 * argumentos, y para que productor y consumidores compartan el mismo converter
 * JSON (fechas ISO-8601, sin serialización Java).
 *
 * <p>Cada cola tiene su DLQ por exchange por defecto. El mensaje se rechaza
 * hacia la DLQ cuando se agotan los reintentos configurados en
 * {@code spring.rabbitmq.listener.simple.retry}.</p>
 */
@AutoConfiguration
@AutoConfigureBefore(RabbitAutoConfiguration.class)
@ConditionalOnClass(RabbitTemplate.class)
public class RegistrationMessagingConfig {

    @Bean
    @ConditionalOnMissingBean(name = "userTopicExchange")
    public TopicExchange userTopicExchange() {
        return new TopicExchange(RegistrationMessagingConstants.EXCHANGE, true, false);
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistrationStatusPublisher registrationStatusPublisher(RabbitTemplate rabbitTemplate) {
        return new RegistrationStatusPublisher(rabbitTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageConverter registrationJsonMessageConverter() {
        JsonMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public Queue userAzureSyncQueue() {
        return conDlq(RegistrationMessagingConstants.AZURE_QUEUE, RegistrationMessagingConstants.AZURE_DLQ);
    }

    @Bean
    public Queue userRegistrationStatusQueue() {
        return conDlq(RegistrationMessagingConstants.STATUS_QUEUE, RegistrationMessagingConstants.STATUS_DLQ);
    }

    @Bean
    public Queue userCredentialsNotifyQueue() {
        return conDlq(RegistrationMessagingConstants.CREDENTIALS_QUEUE, RegistrationMessagingConstants.CREDENTIALS_DLQ);
    }

    @Bean
    public Queue msEstudiantesQueue() {
        return conDlq(RegistrationMessagingConstants.ESTUDIANTES_QUEUE, RegistrationMessagingConstants.ESTUDIANTES_DLQ);
    }

    @Bean
    public Queue msDocentesQueue() {
        return conDlq(RegistrationMessagingConstants.DOCENTES_QUEUE, RegistrationMessagingConstants.DOCENTES_DLQ);
    }

    @Bean
    public Queue msApoderadosQueue() {
        return conDlq(RegistrationMessagingConstants.APODERADOS_QUEUE, RegistrationMessagingConstants.APODERADOS_DLQ);
    }

    @Bean
    public Queue userAzureSyncDlq() {
        return QueueBuilder.durable(RegistrationMessagingConstants.AZURE_DLQ).build();
    }

    @Bean
    public Queue userRegistrationStatusDlq() {
        return QueueBuilder.durable(RegistrationMessagingConstants.STATUS_DLQ).build();
    }

    @Bean
    public Queue userCredentialsNotifyDlq() {
        return QueueBuilder.durable(RegistrationMessagingConstants.CREDENTIALS_DLQ).build();
    }

    @Bean
    public Queue msEstudiantesDlq() {
        return QueueBuilder.durable(RegistrationMessagingConstants.ESTUDIANTES_DLQ).build();
    }

    @Bean
    public Queue msDocentesDlq() {
        return QueueBuilder.durable(RegistrationMessagingConstants.DOCENTES_DLQ).build();
    }

    @Bean
    public Queue msApoderadosDlq() {
        return QueueBuilder.durable(RegistrationMessagingConstants.APODERADOS_DLQ).build();
    }

    @Bean
    public Binding bindingAzureSync(Queue userAzureSyncQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(userAzureSyncQueue).to(userTopicExchange)
                .with(RegistrationMessagingConstants.RK_AZURE_SYNC);
    }

    @Bean
    public Binding bindingEstudiantes(Queue msEstudiantesQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(msEstudiantesQueue).to(userTopicExchange)
                .with(RegistrationMessagingConstants.RK_REGISTER_ESTUDIANTE);
    }

    @Bean
    public Binding bindingDocentes(Queue msDocentesQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(msDocentesQueue).to(userTopicExchange)
                .with(RegistrationMessagingConstants.RK_REGISTER_DOCENTE);
    }

    @Bean
    public Binding bindingApoderados(Queue msApoderadosQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(msApoderadosQueue).to(userTopicExchange)
                .with(RegistrationMessagingConstants.RK_REGISTER_APODERADO);
    }

    @Bean
    public Binding bindingStatus(Queue userRegistrationStatusQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(userRegistrationStatusQueue).to(userTopicExchange)
                .with(RegistrationMessagingConstants.RK_STATUS);
    }

    @Bean
    public Binding bindingCredentialsNotify(Queue userCredentialsNotifyQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(userCredentialsNotifyQueue).to(userTopicExchange)
                .with(RegistrationMessagingConstants.RK_CREDENTIALS_NOTIFY);
    }

    private Queue conDlq(String nombre, String dlq) {
        return QueueBuilder.durable(nombre)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", "",
                        "x-dead-letter-routing-key", dlq))
                .build();
    }
}

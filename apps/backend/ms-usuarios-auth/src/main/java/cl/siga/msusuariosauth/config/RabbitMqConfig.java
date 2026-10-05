package cl.siga.msusuariosauth.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cl.siga.coreshare.messaging.RegistrationMessagingConstants;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange userTopicExchange() {
        return new TopicExchange(RegistrationMessagingConstants.EXCHANGE, true, false);
    }

    @Bean
    public Queue azureQueue() {
        return queueWithDlq(
                RegistrationMessagingConstants.AZURE_QUEUE,
                RegistrationMessagingConstants.AZURE_DLQ);
    }

    @Bean
    public Queue estudianteQueue() {
        return queueWithDlq(
                RegistrationMessagingConstants.ESTUDIANTES_QUEUE,
                RegistrationMessagingConstants.ESTUDIANTES_DLQ);
    }

    @Bean
    public Queue docenteQueue() {
        return queueWithDlq(
                RegistrationMessagingConstants.DOCENTES_QUEUE,
                RegistrationMessagingConstants.DOCENTES_DLQ);
    }

    @Bean
    public Queue apoderadoQueue() {
        return queueWithDlq(
                RegistrationMessagingConstants.APODERADOS_QUEUE,
                RegistrationMessagingConstants.APODERADOS_DLQ);
    }

    @Bean
    public Queue statusQueue() {
        return queueWithDlq(
                RegistrationMessagingConstants.STATUS_QUEUE,
                RegistrationMessagingConstants.STATUS_DLQ);
    }

    @Bean
    public Queue azureDlq() {
        return Queue.durable(RegistrationMessagingConstants.AZURE_DLQ).build();
    }

    @Bean
    public Queue estudianteDlq() {
        return Queue.durable(RegistrationMessagingConstants.ESTUDIANTES_DLQ).build();
    }

    @Bean
    public Queue docenteDlq() {
        return Queue.durable(RegistrationMessagingConstants.DOCENTES_DLQ).build();
    }

    @Bean
    public Queue apoderadoDlq() {
        return Queue.durable(RegistrationMessagingConstants.APODERADOS_DLQ).build();
    }

    @Bean
    public Queue statusDlq() {
        return Queue.durable(RegistrationMessagingConstants.STATUS_DLQ).build();
    }

    @Bean
    public Binding bindingAzure(Queue azureQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(azureQueue).to(userTopicExchange).with("user.register.*");
    }

    @Bean
    public Binding bindingEstudiante(Queue estudianteQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(estudianteQueue)
                .to(userTopicExchange)
                .with(RegistrationMessagingConstants.RK_REGISTER_ESTUDIANTE);
    }

    @Bean
    public Binding bindingDocente(Queue docenteQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(docenteQueue)
                .to(userTopicExchange)
                .with(RegistrationMessagingConstants.RK_REGISTER_DOCENTE);
    }

    @Bean
    public Binding bindingApoderado(Queue apoderadoQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(apoderadoQueue)
                .to(userTopicExchange)
                .with(RegistrationMessagingConstants.RK_REGISTER_APODERADO);
    }

    @Bean
    public Binding bindingStatus(Queue statusQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(statusQueue)
                .to(userTopicExchange)
                .with(RegistrationMessagingConstants.RK_STATUS);
    }

    private Queue queueWithDlq(String queueName, String dlqName) {
        return Queue.durable(queueName)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", "",
                        "x-dead-letter-routing-key", dlqName))
                .build();
    }
}

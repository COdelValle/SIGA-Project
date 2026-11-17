package cl.siga.coreshare.messaging;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cl.siga.coreshare.mensajeria.NotificacionesMensajeriaProperties;
import cl.siga.coreshare.mensajeria.RegistroMensajeriaProperties;
import lombok.RequiredArgsConstructor;

/**
 * Topología compartida del registro asíncrono. Se activa solo en los servicios
 * que incluyen AMQP en el classpath y habilitan
 * {@code siga.mensajeria.registro.topology-enabled} (ms-usuarios-auth,
 * ms-estudiantes, ms-docentes y ms-apoderados) para que todos declaren las
 * mismas colas y argumentos, y para que productor y consumidores compartan el
 * mismo converter JSON (fechas ISO-8601, sin serialización Java).
 *
 * <p>Los nombres se inyectan desde configuración para poder modificarlos sin
 * recompilar.</p>
 */
@AutoConfiguration
@AutoConfigureBefore(RabbitAutoConfiguration.class)
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "siga.mensajeria.registro", name = "topology-enabled", havingValue = "true")
@RequiredArgsConstructor
public class RegistrationMessagingConfig {

    private final RegistroMensajeriaProperties registro;
    private final NotificacionesMensajeriaProperties notificaciones;

    @Bean
    @ConditionalOnMissingBean(name = "userTopicExchange")
    public TopicExchange userTopicExchange() {
        return new TopicExchange(registro.getExchange(), true, false);
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistrationStatusPublisher registrationStatusPublisher(RabbitTemplate rabbitTemplate) {
        return new RegistrationStatusPublisher(rabbitTemplate, registro);
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
        return conDlq(registro.getAzureQueue(), registro.getAzureDlq());
    }

    @Bean
    public Queue userRegistrationStatusQueue() {
        return conDlq(registro.getStatusQueue(), registro.getStatusDlq());
    }

    @Bean
    public Queue userCredentialsNotifyQueue() {
        return conDlq(registro.getCredentialsQueue(), registro.getCredentialsDlq());
    }

    @Bean
    public Queue msEstudiantesQueue() {
        return conDlq(registro.getEstudiantesQueue(), registro.getEstudiantesDlq());
    }

    @Bean
    public Queue msDocentesQueue() {
        return conDlq(registro.getDocentesQueue(), registro.getDocentesDlq());
    }

    @Bean
    public Queue msApoderadosQueue() {
        return conDlq(registro.getApoderadosQueue(), registro.getApoderadosDlq());
    }

    @Bean
    public Queue userAzureSyncDlq() {
        return QueueBuilder.durable(registro.getAzureDlq()).build();
    }

    @Bean
    public Queue userRegistrationStatusDlq() {
        return QueueBuilder.durable(registro.getStatusDlq()).build();
    }

    @Bean
    public Queue userCredentialsNotifyDlq() {
        return QueueBuilder.durable(registro.getCredentialsDlq()).build();
    }

    @Bean
    public Queue msEstudiantesDlq() {
        return QueueBuilder.durable(registro.getEstudiantesDlq()).build();
    }

    @Bean
    public Queue msDocentesDlq() {
        return QueueBuilder.durable(registro.getDocentesDlq()).build();
    }

    @Bean
    public Queue msApoderadosDlq() {
        return QueueBuilder.durable(registro.getApoderadosDlq()).build();
    }

    @Bean
    public Binding bindingAzureSync(Queue userAzureSyncQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(userAzureSyncQueue).to(userTopicExchange)
                .with(registro.getRkAzureSync());
    }

    @Bean
    public Binding bindingEstudiantes(Queue msEstudiantesQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(msEstudiantesQueue).to(userTopicExchange)
                .with(registro.getRkRegisterEstudiante());
    }

    @Bean
    public Binding bindingDocentes(Queue msDocentesQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(msDocentesQueue).to(userTopicExchange)
                .with(registro.getRkRegisterDocente());
    }

    @Bean
    public Binding bindingApoderados(Queue msApoderadosQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(msApoderadosQueue).to(userTopicExchange)
                .with(registro.getRkRegisterApoderado());
    }

    @Bean
    public Binding bindingStatus(Queue userRegistrationStatusQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(userRegistrationStatusQueue).to(userTopicExchange)
                .with(registro.getRkStatus());
    }

    @Bean
    public Binding bindingCredentialsNotify(Queue userCredentialsNotifyQueue, TopicExchange userTopicExchange) {
        return BindingBuilder.bind(userCredentialsNotifyQueue).to(userTopicExchange)
                .with(registro.getRkCredentialsNotify());
    }

    @Bean
    public Binding bindingAzureSyncDlq(Queue userAzureSyncDlq, DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder.bind(userAzureSyncDlq).to(sigaDeadLetterExchange)
                .with(registro.getAzureDlq());
    }

    @Bean
    public Binding bindingEstudiantesDlq(Queue msEstudiantesDlq, DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder.bind(msEstudiantesDlq).to(sigaDeadLetterExchange)
                .with(registro.getEstudiantesDlq());
    }

    @Bean
    public Binding bindingDocentesDlq(Queue msDocentesDlq, DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder.bind(msDocentesDlq).to(sigaDeadLetterExchange)
                .with(registro.getDocentesDlq());
    }

    @Bean
    public Binding bindingApoderadosDlq(Queue msApoderadosDlq, DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder.bind(msApoderadosDlq).to(sigaDeadLetterExchange)
                .with(registro.getApoderadosDlq());
    }

    @Bean
    public Binding bindingStatusDlq(Queue userRegistrationStatusDlq, DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder.bind(userRegistrationStatusDlq).to(sigaDeadLetterExchange)
                .with(registro.getStatusDlq());
    }

    @Bean
    public Binding bindingCredentialsNotifyDlq(Queue userCredentialsNotifyDlq, DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder.bind(userCredentialsNotifyDlq).to(sigaDeadLetterExchange)
                .with(registro.getCredentialsDlq());
    }

    private Queue conDlq(String nombre, String dlq) {
        return QueueBuilder.durable(nombre)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", notificaciones.getExchangeDlq(),
                        "x-dead-letter-routing-key", dlq))
                .build();
    }
}

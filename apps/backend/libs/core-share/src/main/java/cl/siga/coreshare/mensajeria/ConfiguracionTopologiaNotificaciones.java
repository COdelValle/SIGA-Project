package cl.siga.coreshare.mensajeria;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import lombok.RequiredArgsConstructor;

/**
 * Topología compartida de la mensajería académica (notas, asistencias y
 * evaluaciones). Productores y consumidor declaran exactamente lo mismo, de
 * modo que RabbitMQ reutiliza las definiciones y no pueden divergir.
 *
 * <p>Los nombres se inyectan desde configuración para poder modificarlos sin
 * recompilar.</p>
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@RequiredArgsConstructor
public class ConfiguracionTopologiaNotificaciones {

    private final NotificacionesMensajeriaProperties properties;

    @Bean
    @ConditionalOnMissingBean(name = "sigaDeadLetterExchange")
    public DirectExchange sigaDeadLetterExchange() {
        return ExchangeBuilder.directExchange(properties.getExchangeDlq()).durable(true).build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "intercambioNotificaciones")
    public TopicExchange intercambioNotificaciones() {
        return ExchangeBuilder.topicExchange(properties.getExchangeNotificaciones()).durable(true).build();
    }

    @Bean
    public Queue colaNotificacionesEvaluaciones() {
        return colaConDlq(properties.getColaEvaluaciones(), properties.getDlqEvaluaciones());
    }

    @Bean
    public Queue colaNotificacionesEvaluacionesDlq() {
        return QueueBuilder.durable(properties.getDlqEvaluaciones()).build();
    }

    @Bean
    public Queue colaNotificacionesAsistencias() {
        return colaConDlq(properties.getColaAsistencias(), properties.getDlqAsistencias());
    }

    @Bean
    public Queue colaNotificacionesAsistenciasDlq() {
        return QueueBuilder.durable(properties.getDlqAsistencias()).build();
    }

    @Bean
    public Queue colaRiesgoInasistencia() {
        return colaConDlq(properties.getColaRiesgoInasistencia(), properties.getDlqRiesgoInasistencia());
    }

    @Bean
    public Queue colaRiesgoInasistenciaDlq() {
        return QueueBuilder.durable(properties.getDlqRiesgoInasistencia()).build();
    }

    @Bean
    public Queue colaRiesgoRendimiento() {
        return colaConDlq(properties.getColaRiesgoRendimiento(), properties.getDlqRiesgoRendimiento());
    }

    @Bean
    public Queue colaRiesgoRendimientoDlq() {
        return QueueBuilder.durable(properties.getDlqRiesgoRendimiento()).build();
    }

    @Bean
    public Queue colaCambioEstadoAcademico() {
        return colaConDlq(properties.getColaCambioEstadoAcademico(),
            properties.getDlqCambioEstadoAcademico());
    }

    @Bean
    public Queue colaCambioEstadoAcademicoDlq() {
        return QueueBuilder.durable(properties.getDlqCambioEstadoAcademico()).build();
    }

    @Bean
    public Queue colaNotificacionesNotas() {
        return colaConDlq(properties.getColaNotas(), properties.getDlqNotas());
    }

    @Bean
    public Queue colaNotificacionesNotasDlq() {
        return QueueBuilder.durable(properties.getDlqNotas()).build();
    }

    @Bean
    public Binding enlaceEvaluaciones(Queue colaNotificacionesEvaluaciones,
                                      TopicExchange intercambioNotificaciones) {
        return BindingBuilder
            .bind(colaNotificacionesEvaluaciones)
            .to(intercambioNotificaciones)
            .with(properties.getPatronEvaluacion());
    }

    @Bean
    public Binding enlaceAsistencias(Queue colaNotificacionesAsistencias,
                                     TopicExchange intercambioNotificaciones) {
        return BindingBuilder
            .bind(colaNotificacionesAsistencias)
            .to(intercambioNotificaciones)
            .with(properties.getPatronAsistencia());
    }

    @Bean
    public Binding enlaceRiesgoInasistencia(Queue colaRiesgoInasistencia,
                                            TopicExchange intercambioNotificaciones) {
        return BindingBuilder.bind(colaRiesgoInasistencia).to(intercambioNotificaciones)
            .with(properties.getClaveRiesgoInasistencia());
    }

    @Bean
    public Binding enlaceRiesgoRendimiento(Queue colaRiesgoRendimiento,
                                           TopicExchange intercambioNotificaciones) {
        return BindingBuilder.bind(colaRiesgoRendimiento).to(intercambioNotificaciones)
            .with(properties.getClaveRiesgoRendimiento());
    }

    @Bean
    public Binding enlaceCambioEstadoAcademico(Queue colaCambioEstadoAcademico,
                                               TopicExchange intercambioNotificaciones) {
        return BindingBuilder.bind(colaCambioEstadoAcademico).to(intercambioNotificaciones)
            .with(properties.getClaveCambioEstadoAcademico());
    }

    @Bean
    public Binding enlaceNotas(Queue colaNotificacionesNotas,
                               TopicExchange intercambioNotificaciones) {
        return BindingBuilder
            .bind(colaNotificacionesNotas)
            .to(intercambioNotificaciones)
            .with(properties.getPatronNota());
    }

    @Bean
    public Binding enlaceDlqEvaluaciones(Queue colaNotificacionesEvaluacionesDlq,
                                         DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder
            .bind(colaNotificacionesEvaluacionesDlq)
            .to(sigaDeadLetterExchange)
            .with(properties.getDlqEvaluaciones());
    }

    @Bean
    public Binding enlaceDlqAsistencias(Queue colaNotificacionesAsistenciasDlq,
                                        DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder
            .bind(colaNotificacionesAsistenciasDlq)
            .to(sigaDeadLetterExchange)
            .with(properties.getDlqAsistencias());
    }

    @Bean
    public Binding enlaceDlqRiesgoInasistencia(Queue colaRiesgoInasistenciaDlq,
                                               DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder.bind(colaRiesgoInasistenciaDlq).to(sigaDeadLetterExchange)
            .with(properties.getDlqRiesgoInasistencia());
    }

    @Bean
    public Binding enlaceDlqRiesgoRendimiento(Queue colaRiesgoRendimientoDlq,
                                              DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder.bind(colaRiesgoRendimientoDlq).to(sigaDeadLetterExchange)
            .with(properties.getDlqRiesgoRendimiento());
    }

    @Bean
    public Binding enlaceDlqCambioEstadoAcademico(Queue colaCambioEstadoAcademicoDlq,
                                                  DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder.bind(colaCambioEstadoAcademicoDlq).to(sigaDeadLetterExchange)
            .with(properties.getDlqCambioEstadoAcademico());
    }

    @Bean
    public Binding enlaceDlqNotas(Queue colaNotificacionesNotasDlq,
                                  DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder
            .bind(colaNotificacionesNotasDlq)
            .to(sigaDeadLetterExchange)
            .with(properties.getDlqNotas());
    }

    private Queue colaConDlq(String nombre, String dlq) {
        return QueueBuilder.durable(nombre)
            .withArguments(Map.of(
                "x-dead-letter-exchange", properties.getExchangeDlq(),
                "x-dead-letter-routing-key", dlq))
            .build();
    }
}

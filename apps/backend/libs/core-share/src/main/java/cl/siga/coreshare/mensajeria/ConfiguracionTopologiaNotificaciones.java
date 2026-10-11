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

/**
 * Topología compartida de la mensajería académica (notas, asistencias y
 * evaluaciones). Productores y consumidor declaran exactamente lo mismo, de
 * modo que RabbitMQ reutiliza las definiciones y no pueden divergir:
 *
 * <ul>
 *   <li>Topic exchange {@code intercambio-notificaciones}: reparte por patrón
 *       ({@code nota.*}, {@code asistencia.*}, {@code evaluacion.*}).</li>
 *   <li>Una cola durable por tipo de evento, cada una con su DLQ.</li>
 *   <li>Direct exchange {@code siga.dlx.direct}: las colas derivan aquí los
 *       mensajes rechazados y cada DLQ se enlaza con su nombre como routing
 *       key.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
public class ConfiguracionTopologiaNotificaciones {

    @Bean
    @ConditionalOnMissingBean(name = "sigaDeadLetterExchange")
    public DirectExchange sigaDeadLetterExchange() {
        return ExchangeBuilder.directExchange(NombresMensajeria.INTERCAMBIO_DLQ).durable(true).build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "intercambioNotificaciones")
    public TopicExchange intercambioNotificaciones() {
        return ExchangeBuilder.topicExchange(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES).durable(true).build();
    }

    @Bean
    public Queue colaNotificacionesEvaluaciones() {
        return colaConDlq(NombresMensajeria.COLA_EVALUACIONES, NombresMensajeria.DLQ_EVALUACIONES);
    }

    @Bean
    public Queue colaNotificacionesEvaluacionesDlq() {
        return QueueBuilder.durable(NombresMensajeria.DLQ_EVALUACIONES).build();
    }

    @Bean
    public Queue colaNotificacionesAsistencias() {
        return colaConDlq(NombresMensajeria.COLA_ASISTENCIAS, NombresMensajeria.DLQ_ASISTENCIAS);
    }

    @Bean
    public Queue colaNotificacionesAsistenciasDlq() {
        return QueueBuilder.durable(NombresMensajeria.DLQ_ASISTENCIAS).build();
    }

    @Bean
    public Queue colaNotificacionesNotas() {
        return colaConDlq(NombresMensajeria.COLA_NOTAS, NombresMensajeria.DLQ_NOTAS);
    }

    @Bean
    public Queue colaNotificacionesNotasDlq() {
        return QueueBuilder.durable(NombresMensajeria.DLQ_NOTAS).build();
    }

    @Bean
    public Binding enlaceEvaluaciones(Queue colaNotificacionesEvaluaciones,
                                      TopicExchange intercambioNotificaciones) {
        return BindingBuilder
            .bind(colaNotificacionesEvaluaciones)
            .to(intercambioNotificaciones)
            .with(NombresMensajeria.PATRON_EVALUACION);
    }

    @Bean
    public Binding enlaceAsistencias(Queue colaNotificacionesAsistencias,
                                     TopicExchange intercambioNotificaciones) {
        return BindingBuilder
            .bind(colaNotificacionesAsistencias)
            .to(intercambioNotificaciones)
            .with(NombresMensajeria.PATRON_ASISTENCIA);
    }

    @Bean
    public Binding enlaceNotas(Queue colaNotificacionesNotas,
                               TopicExchange intercambioNotificaciones) {
        return BindingBuilder
            .bind(colaNotificacionesNotas)
            .to(intercambioNotificaciones)
            .with(NombresMensajeria.PATRON_NOTA);
    }

    @Bean
    public Binding enlaceDlqEvaluaciones(Queue colaNotificacionesEvaluacionesDlq,
                                         DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder
            .bind(colaNotificacionesEvaluacionesDlq)
            .to(sigaDeadLetterExchange)
            .with(NombresMensajeria.DLQ_EVALUACIONES);
    }

    @Bean
    public Binding enlaceDlqAsistencias(Queue colaNotificacionesAsistenciasDlq,
                                        DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder
            .bind(colaNotificacionesAsistenciasDlq)
            .to(sigaDeadLetterExchange)
            .with(NombresMensajeria.DLQ_ASISTENCIAS);
    }

    @Bean
    public Binding enlaceDlqNotas(Queue colaNotificacionesNotasDlq,
                                  DirectExchange sigaDeadLetterExchange) {
        return BindingBuilder
            .bind(colaNotificacionesNotasDlq)
            .to(sigaDeadLetterExchange)
            .with(NombresMensajeria.DLQ_NOTAS);
    }

    private Queue colaConDlq(String nombre, String dlq) {
        return QueueBuilder.durable(nombre)
            .withArguments(Map.of(
                "x-dead-letter-exchange", NombresMensajeria.INTERCAMBIO_DLQ,
                "x-dead-letter-routing-key", dlq))
            .build();
    }
}

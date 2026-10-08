package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.mensajeria.NombresMensajeria;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * El Consumidor declara lo mismo que el productor (intercambio + cola + enlace).
 * Si el productor ya los creó, RabbitMQ los reutiliza; si no, los crea este.
 * Patrón "evaluacion.*": una sola cola recibe creada, actualizada y eliminada.
 */
@Configuration
public class ConfiguracionMensajeriaNotificaciones {

    @Bean
    public TopicExchange intercambioNotificaciones() {
        return new TopicExchange(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES, true, false);
    }

    @Bean
    public Queue colaNotificacionesEvaluaciones() {
        return QueueBuilder.durable(NombresMensajeria.COLA_EVALUACIONES)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", "",
                        "x-dead-letter-routing-key", NombresMensajeria.DLQ_EVALUACIONES))
                .build();
    }

    @Bean
    public Queue colaNotificacionesEvaluacionesDlq() {
        return QueueBuilder.durable(NombresMensajeria.DLQ_EVALUACIONES).build();
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
    public Queue colaNotificacionesAsistencias() {
        return QueueBuilder.durable(NombresMensajeria.COLA_ASISTENCIAS)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", "",
                        "x-dead-letter-routing-key", NombresMensajeria.DLQ_ASISTENCIAS))
                .build();
    }

    @Bean
    public Queue colaNotificacionesAsistenciasDlq() {
        return QueueBuilder.durable(NombresMensajeria.DLQ_ASISTENCIAS).build();
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
    public Queue colaNotificacionesNotas() {
        return QueueBuilder.durable(NombresMensajeria.COLA_NOTAS)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", "",
                        "x-dead-letter-routing-key", NombresMensajeria.DLQ_NOTAS))
                .build();
    }

    @Bean
    public Queue colaNotificacionesNotasDlq() {
        return QueueBuilder.durable(NombresMensajeria.DLQ_NOTAS).build();
    }

    @Bean
    public Binding enlaceNotas(Queue colaNotificacionesNotas,
                               TopicExchange intercambioNotificaciones) {
        return BindingBuilder
                .bind(colaNotificacionesNotas)
                .to(intercambioNotificaciones)
                .with(NombresMensajeria.PATRON_NOTA);
    }
}

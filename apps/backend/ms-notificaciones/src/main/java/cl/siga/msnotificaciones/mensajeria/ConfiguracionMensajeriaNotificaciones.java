package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.mensajeria.NombresMensajeria;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return new Queue(NombresMensajeria.COLA_EVALUACIONES, true);
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
        return new Queue(NombresMensajeria.COLA_ASISTENCIAS, true);
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
        return new Queue(NombresMensajeria.COLA_NOTAS, true);
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

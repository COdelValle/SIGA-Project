package cl.siga.msevaluaciones.mensajeria;

import cl.siga.coreshare.mensajeria.NombresMensajeria;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el lado productor de tu imagen para evaluaciones:
 * - Intercambio Topic (el naranjo): reparte por patrón.
 * - Cola de evaluaciones (tu Queue 2): espera los 3 eventos.
 * - Enlace con patrón "evaluacion.*": deja pasar
 *   evaluacion.creada, evaluacion.actualizada y evaluacion.eliminada.
 * RabbitMQ crea todo esto solo al levantar si no existe (durable = sobrevive a reinicios).
 */
@Configuration
public class ConfiguracionMensajeriaEvaluacion {

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
}

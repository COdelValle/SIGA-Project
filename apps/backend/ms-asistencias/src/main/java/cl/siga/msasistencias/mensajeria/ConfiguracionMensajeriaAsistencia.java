package cl.siga.msasistencias.mensajeria;

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
 * Declara el lado productor para asistencias, igual que en evaluaciones:
 * - Usa el mismo intercambio Topic compartido (intercambio-notificaciones).
 * - Cola nueva: cola-notificaciones-asistencias.
 * - Enlace con patrón "asistencia.*".
 */
@Configuration
public class ConfiguracionMensajeriaAsistencia {

    @Bean
    public TopicExchange intercambioNotificaciones() {
        return new TopicExchange(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES, true, false);
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
}

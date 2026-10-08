package cl.siga.msnotas.mensajeria;

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
 * Lado productor para notas: el mismo intercambio Topic compartido,
 * cola nueva (cola-notificaciones-notas) y enlace con patrón "nota.*".
 */
@Configuration
public class ConfiguracionMensajeriaNota {

    @Bean
    public TopicExchange intercambioNotificaciones() {
        return new TopicExchange(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES, true, false);
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

package cl.siga.msnotas.mensajeria;

import cl.siga.coreshare.mensajeria.NombresMensajeria;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

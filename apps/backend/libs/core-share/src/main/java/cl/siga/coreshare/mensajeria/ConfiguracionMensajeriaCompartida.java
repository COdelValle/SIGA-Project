package cl.siga.coreshare.mensajeria;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * El Intermediario de tu imagen habla en bytes; este conversor hace que
 * productor y consumidor hablen en JSON con nuestros records
 * (EventoEvaluacion y los 2 futuros) sin código repetido.
 * Registrado en AutoConfiguration.imports como el resto de core-share.
 */
@AutoConfiguration
public class ConfiguracionMensajeriaCompartida {

    @Bean
    public MessageConverter conversorMensajesJson() {
        return new Jackson2JsonMessageConverter();
    }
}

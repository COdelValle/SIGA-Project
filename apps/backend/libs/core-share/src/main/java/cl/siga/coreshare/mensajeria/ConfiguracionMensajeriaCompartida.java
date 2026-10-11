package cl.siga.coreshare.mensajeria;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * El Intermediario de tu imagen habla en bytes; este conversor hace que
 * productor y consumidor hablen en JSON con nuestros records
 * (EventoEvaluacion y los 2 futuros) sin código repetido.
 * Registrado en AutoConfiguration.imports como el resto de core-share.
 *
 * <p>Además expone el {@link RetryTemplate} y el {@link ConfirmadorMensajes}
 * que usan los consumidores para confirmar con ACK/NACK explícitos: los
 * errores transitorios se reintentan en memoria con backoff y los permanentes
 * (evento inválido o reintentos agotados) se derivan a la DLQ.</p>
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
public class ConfiguracionMensajeriaCompartida {

    private static final int MAX_INTENTOS = 3;
    private static final long INTERVALO_INICIAL_MS = 1000;
    private static final double MULTIPLICADOR = 2.0;
    private static final long INTERVALO_MAXIMO_MS = 5000;

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter conversorMensajesJson() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "retryTemplateMensajeria")
    public RetryTemplate retryTemplateMensajeria() {
        Map<Class<? extends Throwable>, Boolean> recuperables = new HashMap<>();
        recuperables.put(EventoInvalidoException.class, false);
        SimpleRetryPolicy politica = new SimpleRetryPolicy(MAX_INTENTOS, recuperables, true, true);

        ExponentialBackOffPolicy retroceso = new ExponentialBackOffPolicy();
        retroceso.setInitialInterval(INTERVALO_INICIAL_MS);
        retroceso.setMultiplier(MULTIPLICADOR);
        retroceso.setMaxInterval(INTERVALO_MAXIMO_MS);

        RetryTemplate plantilla = new RetryTemplate();
        plantilla.setRetryPolicy(politica);
        plantilla.setBackOffPolicy(retroceso);
        return plantilla;
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfirmadorMensajes confirmadorMensajes(RetryTemplate retryTemplateMensajeria) {
        return new ConfirmadorMensajes(retryTemplateMensajeria);
    }
}

package cl.siga.coreshare.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ConfiguracionMensajeriaCompartidaTest {

    @Test
    void comparteUnSoloConversorJsonConLaTopologiaDeRegistro() {
        new ApplicationContextRunner()
            .withUserConfiguration(RabbitTemplateMockConfig.class)
            .withConfiguration(AutoConfigurations.of(
                cl.siga.coreshare.messaging.RegistrationMessagingConfig.class,
                ConfiguracionMensajeriaCompartida.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBeansOfType(MessageConverter.class)).hasSize(1);
                assertThat(context.getBean(MessageConverter.class))
                    .isInstanceOf(Jackson2JsonMessageConverter.class);
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class RabbitTemplateMockConfig {
        @Bean
        RabbitTemplate rabbitTemplate() {
            return mock(RabbitTemplate.class);
        }
    }
}

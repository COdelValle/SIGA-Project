package cl.siga.coreshare.mensajeria.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

class NotificationOutboxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(Dependencies.class)
        .withConfiguration(AutoConfigurations.of(NotificationOutboxAutoConfiguration.class));

    @Test
    void creaElOutboxSoloCuandoEstaHabilitado() {
        contextRunner
            .withPropertyValues("siga.notificaciones.outbox.enabled=true", "siga.notificaciones.outbox.delay-ms=60000")
            .run(context -> {
                assertThat(context).hasSingleBean(NotificationEventOutbox.class);
                assertThat(context).hasSingleBean(NotificationOutboxDispatcher.class);
            });

        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(NotificationEventOutbox.class);
            assertThat(context).doesNotHaveBean(NotificationOutboxDispatcher.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class Dependencies {
        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        @Bean
        RabbitTemplate rabbitTemplate() {
            return mock(RabbitTemplate.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}

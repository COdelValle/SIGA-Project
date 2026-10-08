package cl.siga.coreshare.mensajeria.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@AutoConfigureAfter({JdbcTemplateAutoConfiguration.class, RabbitAutoConfiguration.class, JacksonAutoConfiguration.class})
@EnableScheduling
@ConditionalOnClass({JdbcTemplate.class, RabbitTemplate.class})
@ConditionalOnBean({JdbcTemplate.class, RabbitTemplate.class})
@ConditionalOnProperty(prefix = "siga.notificaciones.outbox", name = "enabled", havingValue = "true")
public class NotificationOutboxAutoConfiguration {

    @Bean
    public NotificationEventOutbox notificationEventOutbox(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new NotificationEventOutbox(jdbcTemplate, objectMapper);
    }

    @Bean
    public NotificationOutboxDispatcher notificationOutboxDispatcher(
            JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, RabbitTemplate rabbitTemplate) {
        return new NotificationOutboxDispatcher(jdbcTemplate, objectMapper, rabbitTemplate);
    }
}

package cl.siga.coreshare.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;

class RegistrationMessagingConfigExtraTest {

    private final RegistrationMessagingConfig config = new RegistrationMessagingConfig(
        RegistrationMessagingConfigTest.registro(),
        RegistrationMessagingConfigTest.notificaciones());

    @Test
    void creaElPublicadorDeEstadoConElRabbitTemplate() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        RegistrationStatusPublisher publisher = config.registrationStatusPublisher(rabbitTemplate);

        assertThat(publisher).isNotNull();
    }

    @Test
    void elConverterJsonSerializaFechasComoIso() {
        MessageConverter converter = config.registrationJsonMessageConverter();
        UserRegistrationEventDTO evento = new UserRegistrationEventDTO(
                UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION,
                "evt-1", "proc-1", "corr-1", "ana@test.com", "Ana",
                Rol.ESTUDIANTE, null, null, OffsetDateTime.parse("2026-10-06T15:30:00-03:00"));

        Message mensaje = converter.toMessage(evento, new org.springframework.amqp.core.MessageProperties());
        Object leido = converter.fromMessage(mensaje);

        assertThat(leido).isInstanceOf(UserRegistrationEventDTO.class);
        assertThat(((UserRegistrationEventDTO) leido).processId()).isEqualTo("proc-1");
        assertThat(((UserRegistrationEventDTO) leido).occurredAt()).isNotNull();
        assertThat(mensaje.getMessageProperties().getContentType()).contains("json");
    }
}

package cl.siga.coreshare.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.messaging.RegistrationMessagingConstants;

class ConfiguracionNombresRabbitMQTest {

    @Test
    void aliasesDeCadaFlujoApuntanALaConfiguracionCentral() {
        assertThat(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES)
            .isEqualTo(ConfiguracionNombresRabbitMQ.Academico.EXCHANGE_NOTIFICACIONES);
        assertThat(NombresMensajeria.COLA_EVALUACIONES)
            .isEqualTo(ConfiguracionNombresRabbitMQ.Academico.COLA_EVALUACIONES);
        assertThat(NombresMensajeria.PATRON_NOTA)
            .isEqualTo(ConfiguracionNombresRabbitMQ.Academico.PATRON_NOTA);
        assertThat(RegistrationMessagingConstants.EXCHANGE)
            .isEqualTo(ConfiguracionNombresRabbitMQ.Registro.EXCHANGE);
        assertThat(RegistrationMessagingConstants.ESTUDIANTES_QUEUE)
            .isEqualTo(ConfiguracionNombresRabbitMQ.Registro.ESTUDIANTES_QUEUE);
        assertThat(RegistrationMessagingConstants.HEADER_CORRELATION_ID)
            .isEqualTo(ConfiguracionNombresRabbitMQ.Registro.HEADER_CORRELATION_ID);
    }
}

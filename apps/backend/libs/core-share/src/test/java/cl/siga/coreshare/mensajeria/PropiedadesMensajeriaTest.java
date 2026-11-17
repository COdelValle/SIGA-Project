package cl.siga.coreshare.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class PropiedadesMensajeriaTest {

    @Test
    void enlazaLosNombresDelRegistroDesdeElEntorno() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("siga.mensajeria.registro.exchange", "exchange-test")
            .withProperty("siga.mensajeria.registro.estudiantes-queue", "cola-test")
            .withProperty("siga.mensajeria.registro.estudiantes-dlq", "cola-test.dlq")
            .withProperty("siga.mensajeria.registro.header-correlation-id", "X-Test-Correlation");

        RegistroMensajeriaProperties properties = Binder.get(environment)
            .bind("siga.mensajeria.registro", Bindable.of(RegistroMensajeriaProperties.class))
            .get();

        assertThat(properties.getExchange()).isEqualTo("exchange-test");
        assertThat(properties.getEstudiantesQueue()).isEqualTo("cola-test");
        assertThat(properties.getEstudiantesDlq()).isEqualTo("cola-test.dlq");
        assertThat(properties.getHeaderCorrelationId()).isEqualTo("X-Test-Correlation");
    }

    @Test
    void enlazaLosNombresDeNotificacionesDesdeElEntorno() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("siga.mensajeria.notificaciones.cola-notas", "cola-notas-test")
            .withProperty("siga.mensajeria.notificaciones.dlq-notas", "cola-notas-test.dlq")
            .withProperty("siga.mensajeria.notificaciones.clave-nota-creada", "nota.test");

        NotificacionesMensajeriaProperties properties = Binder.get(environment)
            .bind("siga.mensajeria.notificaciones",
                Bindable.of(NotificacionesMensajeriaProperties.class))
            .get();

        assertThat(properties.getColaNotas()).isEqualTo("cola-notas-test");
        assertThat(properties.getDlqNotas()).isEqualTo("cola-notas-test.dlq");
        assertThat(properties.getClaveNotaCreada()).isEqualTo("nota.test");
    }
}


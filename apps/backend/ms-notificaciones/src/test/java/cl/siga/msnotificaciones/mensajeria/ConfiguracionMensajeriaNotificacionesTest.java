package cl.siga.msnotificaciones.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import cl.siga.coreshare.mensajeria.NombresMensajeria;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

class ConfiguracionMensajeriaNotificacionesTest {

    private final ConfiguracionMensajeriaNotificaciones configuracion =
        new ConfiguracionMensajeriaNotificaciones();

    @Test
    void exchangeYColasSonDurablesYCadaColaDeEventosTieneSuDlq() {
        assertThat(configuracion.intercambioNotificaciones().getName())
            .isEqualTo(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES);
        assertThat(configuracion.intercambioNotificaciones().isDurable()).isTrue();

        verificarCola(configuracion.colaNotificacionesEvaluaciones(), NombresMensajeria.COLA_EVALUACIONES,
            NombresMensajeria.DLQ_EVALUACIONES);
        verificarCola(configuracion.colaNotificacionesAsistencias(), NombresMensajeria.COLA_ASISTENCIAS,
            NombresMensajeria.DLQ_ASISTENCIAS);
        verificarCola(configuracion.colaNotificacionesNotas(), NombresMensajeria.COLA_NOTAS,
            NombresMensajeria.DLQ_NOTAS);

        assertThat(configuracion.colaNotificacionesEvaluacionesDlq().isDurable()).isTrue();
        assertThat(configuracion.colaNotificacionesAsistenciasDlq().isDurable()).isTrue();
        assertThat(configuracion.colaNotificacionesNotasDlq().isDurable()).isTrue();
    }

    private static void verificarCola(Queue cola, String nombre, String dlq) {
        assertThat(cola.getName()).isEqualTo(nombre);
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
            .containsEntry("x-dead-letter-exchange", "")
            .containsEntry("x-dead-letter-routing-key", dlq);
    }
}

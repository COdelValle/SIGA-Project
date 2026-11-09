package cl.siga.coreshare.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

class ConfiguracionTopologiaNotificacionesTest {

    private final ConfiguracionTopologiaNotificaciones configuracion = new ConfiguracionTopologiaNotificaciones();

    @Test
    void declaraExchangeTopicYDirectoDurables() {
        assertThat(configuracion.intercambioNotificaciones().getName())
            .isEqualTo(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES);
        assertThat(configuracion.intercambioNotificaciones().getType()).isEqualTo("topic");
        assertThat(configuracion.intercambioNotificaciones().isDurable()).isTrue();

        assertThat(configuracion.sigaDeadLetterExchange().getName())
            .isEqualTo(NombresMensajeria.INTERCAMBIO_DLQ);
        assertThat(configuracion.sigaDeadLetterExchange().getType()).isEqualTo("direct");
        assertThat(configuracion.sigaDeadLetterExchange().isDurable()).isTrue();
    }

    @Test
    void colasDurablesDerivanSusRechazosAlExchangeDirecto() {
        verificarCola(configuracion.colaNotificacionesEvaluaciones(),
            NombresMensajeria.COLA_EVALUACIONES, NombresMensajeria.DLQ_EVALUACIONES);
        verificarCola(configuracion.colaNotificacionesAsistencias(),
            NombresMensajeria.COLA_ASISTENCIAS, NombresMensajeria.DLQ_ASISTENCIAS);
        verificarCola(configuracion.colaNotificacionesNotas(),
            NombresMensajeria.COLA_NOTAS, NombresMensajeria.DLQ_NOTAS);
        verificarCola(configuracion.colaRiesgoInasistencia(),
            NombresMensajeria.COLA_RIESGO_INASISTENCIA, NombresMensajeria.DLQ_RIESGO_INASISTENCIA);
        verificarCola(configuracion.colaRiesgoRendimiento(),
            NombresMensajeria.COLA_RIESGO_RENDIMIENTO, NombresMensajeria.DLQ_RIESGO_RENDIMIENTO);
        verificarCola(configuracion.colaCambioEstadoAcademico(),
            NombresMensajeria.COLA_CAMBIO_ESTADO_ACADEMICO, NombresMensajeria.DLQ_CAMBIO_ESTADO_ACADEMICO);

        assertThat(configuracion.colaNotificacionesEvaluacionesDlq().isDurable()).isTrue();
        assertThat(configuracion.colaNotificacionesAsistenciasDlq().isDurable()).isTrue();
        assertThat(configuracion.colaNotificacionesNotasDlq().isDurable()).isTrue();
        assertThat(configuracion.colaRiesgoInasistenciaDlq().isDurable()).isTrue();
        assertThat(configuracion.colaRiesgoRendimientoDlq().isDurable()).isTrue();
        assertThat(configuracion.colaCambioEstadoAcademicoDlq().isDurable()).isTrue();
    }

    @Test
    void bindingsEnlazanPatronesYColasDlq() {
        Binding evaluaciones = configuracion.enlaceEvaluaciones(
            configuracion.colaNotificacionesEvaluaciones(), configuracion.intercambioNotificaciones());
        assertThat(evaluaciones.getExchange()).isEqualTo(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES);
        assertThat(evaluaciones.getDestination()).isEqualTo(NombresMensajeria.COLA_EVALUACIONES);
        assertThat(evaluaciones.getRoutingKey()).isEqualTo(NombresMensajeria.PATRON_EVALUACION);

        Binding asistencias = configuracion.enlaceAsistencias(
            configuracion.colaNotificacionesAsistencias(), configuracion.intercambioNotificaciones());
        assertThat(asistencias.getRoutingKey()).isEqualTo(NombresMensajeria.PATRON_ASISTENCIA);

        Binding notas = configuracion.enlaceNotas(
            configuracion.colaNotificacionesNotas(), configuracion.intercambioNotificaciones());
        assertThat(notas.getRoutingKey()).isEqualTo(NombresMensajeria.PATRON_NOTA);

        assertThat(configuracion.enlaceRiesgoInasistencia(
            configuracion.colaRiesgoInasistencia(), configuracion.intercambioNotificaciones())
            .getRoutingKey()).isEqualTo(NombresMensajeria.CLAVE_RIESGO_INASISTENCIA);
        assertThat(configuracion.enlaceRiesgoRendimiento(
            configuracion.colaRiesgoRendimiento(), configuracion.intercambioNotificaciones())
            .getRoutingKey()).isEqualTo(NombresMensajeria.CLAVE_RIESGO_RENDIMIENTO);
        assertThat(configuracion.enlaceCambioEstadoAcademico(
            configuracion.colaCambioEstadoAcademico(), configuracion.intercambioNotificaciones())
            .getRoutingKey()).isEqualTo(NombresMensajeria.CLAVE_CAMBIO_ESTADO_ACADEMICO);

        Binding dlqEvaluaciones = configuracion.enlaceDlqEvaluaciones(
            configuracion.colaNotificacionesEvaluacionesDlq(), configuracion.sigaDeadLetterExchange());
        assertThat(dlqEvaluaciones.getExchange()).isEqualTo(NombresMensajeria.INTERCAMBIO_DLQ);
        assertThat(dlqEvaluaciones.getDestination()).isEqualTo(NombresMensajeria.DLQ_EVALUACIONES);
        assertThat(dlqEvaluaciones.getRoutingKey()).isEqualTo(NombresMensajeria.DLQ_EVALUACIONES);

        Binding dlqAsistencias = configuracion.enlaceDlqAsistencias(
            configuracion.colaNotificacionesAsistenciasDlq(), configuracion.sigaDeadLetterExchange());
        assertThat(dlqAsistencias.getRoutingKey()).isEqualTo(NombresMensajeria.DLQ_ASISTENCIAS);

        Binding dlqNotas = configuracion.enlaceDlqNotas(
            configuracion.colaNotificacionesNotasDlq(), configuracion.sigaDeadLetterExchange());
        assertThat(dlqNotas.getRoutingKey()).isEqualTo(NombresMensajeria.DLQ_NOTAS);
    }

    private static void verificarCola(Queue cola, String nombre, String dlq) {
        assertThat(cola.getName()).isEqualTo(nombre);
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
            .containsEntry("x-dead-letter-exchange", NombresMensajeria.INTERCAMBIO_DLQ)
            .containsEntry("x-dead-letter-routing-key", dlq);
    }
}

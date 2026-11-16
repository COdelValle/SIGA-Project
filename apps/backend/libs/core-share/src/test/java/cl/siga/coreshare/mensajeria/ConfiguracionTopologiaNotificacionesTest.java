package cl.siga.coreshare.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

class ConfiguracionTopologiaNotificacionesTest {

    private final NotificacionesMensajeriaProperties properties = propiedades();
    private final ConfiguracionTopologiaNotificaciones configuracion =
        new ConfiguracionTopologiaNotificaciones(properties);

    @Test
    void declaraExchangeTopicYDirectoDurables() {
        assertThat(configuracion.intercambioNotificaciones().getName())
            .isEqualTo(properties.getExchangeNotificaciones());
        assertThat(configuracion.intercambioNotificaciones().getType()).isEqualTo("topic");
        assertThat(configuracion.intercambioNotificaciones().isDurable()).isTrue();

        assertThat(configuracion.sigaDeadLetterExchange().getName())
            .isEqualTo(properties.getExchangeDlq());
        assertThat(configuracion.sigaDeadLetterExchange().getType()).isEqualTo("direct");
        assertThat(configuracion.sigaDeadLetterExchange().isDurable()).isTrue();
    }

    @Test
    void colasDurablesDerivanSusRechazosAlExchangeDirecto() {
        verificarCola(configuracion.colaNotificacionesEvaluaciones(),
            properties.getColaEvaluaciones(), properties.getDlqEvaluaciones());
        verificarCola(configuracion.colaNotificacionesAsistencias(),
            properties.getColaAsistencias(), properties.getDlqAsistencias());
        verificarCola(configuracion.colaNotificacionesNotas(),
            properties.getColaNotas(), properties.getDlqNotas());
        verificarCola(configuracion.colaRiesgoInasistencia(),
            properties.getColaRiesgoInasistencia(), properties.getDlqRiesgoInasistencia());
        verificarCola(configuracion.colaRiesgoRendimiento(),
            properties.getColaRiesgoRendimiento(), properties.getDlqRiesgoRendimiento());
        verificarCola(configuracion.colaCambioEstadoAcademico(),
            properties.getColaCambioEstadoAcademico(), properties.getDlqCambioEstadoAcademico());

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
        assertThat(evaluaciones.getExchange()).isEqualTo(properties.getExchangeNotificaciones());
        assertThat(evaluaciones.getDestination()).isEqualTo(properties.getColaEvaluaciones());
        assertThat(evaluaciones.getRoutingKey()).isEqualTo(properties.getPatronEvaluacion());

        Binding asistencias = configuracion.enlaceAsistencias(
            configuracion.colaNotificacionesAsistencias(), configuracion.intercambioNotificaciones());
        assertThat(asistencias.getRoutingKey()).isEqualTo(properties.getPatronAsistencia());

        Binding notas = configuracion.enlaceNotas(
            configuracion.colaNotificacionesNotas(), configuracion.intercambioNotificaciones());
        assertThat(notas.getRoutingKey()).isEqualTo(properties.getPatronNota());

        assertThat(configuracion.enlaceRiesgoInasistencia(
            configuracion.colaRiesgoInasistencia(), configuracion.intercambioNotificaciones())
            .getRoutingKey()).isEqualTo(properties.getClaveRiesgoInasistencia());
        assertThat(configuracion.enlaceRiesgoRendimiento(
            configuracion.colaRiesgoRendimiento(), configuracion.intercambioNotificaciones())
            .getRoutingKey()).isEqualTo(properties.getClaveRiesgoRendimiento());
        assertThat(configuracion.enlaceCambioEstadoAcademico(
            configuracion.colaCambioEstadoAcademico(), configuracion.intercambioNotificaciones())
            .getRoutingKey()).isEqualTo(properties.getClaveCambioEstadoAcademico());

        Binding dlqEvaluaciones = configuracion.enlaceDlqEvaluaciones(
            configuracion.colaNotificacionesEvaluacionesDlq(), configuracion.sigaDeadLetterExchange());
        assertThat(dlqEvaluaciones.getExchange()).isEqualTo(properties.getExchangeDlq());
        assertThat(dlqEvaluaciones.getDestination()).isEqualTo(properties.getDlqEvaluaciones());
        assertThat(dlqEvaluaciones.getRoutingKey()).isEqualTo(properties.getDlqEvaluaciones());

        Binding dlqAsistencias = configuracion.enlaceDlqAsistencias(
            configuracion.colaNotificacionesAsistenciasDlq(), configuracion.sigaDeadLetterExchange());
        assertThat(dlqAsistencias.getRoutingKey()).isEqualTo(properties.getDlqAsistencias());

        Binding dlqNotas = configuracion.enlaceDlqNotas(
            configuracion.colaNotificacionesNotasDlq(), configuracion.sigaDeadLetterExchange());
        assertThat(dlqNotas.getRoutingKey()).isEqualTo(properties.getDlqNotas());
    }

    @Test
    void losNombresSePuedenCambiarSinModificarJava() {
        NotificacionesMensajeriaProperties personalizadas = propiedades();
        personalizadas.setColaNotas("cola-notas-personalizada");
        personalizadas.setDlqNotas("cola-notas-personalizada.dlq");

        ConfiguracionTopologiaNotificaciones otra =
            new ConfiguracionTopologiaNotificaciones(personalizadas);

        assertThat(otra.colaNotificacionesNotas().getName())
            .isEqualTo("cola-notas-personalizada");
        assertThat(otra.enlaceNotas(otra.colaNotificacionesNotas(), otra.intercambioNotificaciones())
            .getDestination()).isEqualTo("cola-notas-personalizada");
    }

    private static void verificarCola(Queue cola, String nombre, String dlq) {
        assertThat(cola.getName()).isEqualTo(nombre);
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
            .containsEntry("x-dead-letter-exchange", "siga.dlx.direct")
            .containsEntry("x-dead-letter-routing-key", dlq);
    }

    static NotificacionesMensajeriaProperties propiedades() {
        NotificacionesMensajeriaProperties p = new NotificacionesMensajeriaProperties();
        p.setExchangeNotificaciones("intercambio-notificaciones");
        p.setExchangeDlq("siga.dlx.direct");
        p.setColaEvaluaciones("cola-notificaciones-evaluaciones");
        p.setDlqEvaluaciones("cola-notificaciones-evaluaciones.dlq");
        p.setColaAsistencias("cola-notificaciones-asistencias");
        p.setDlqAsistencias("cola-notificaciones-asistencias.dlq");
        p.setColaNotas("cola-notificaciones-notas");
        p.setDlqNotas("cola-notificaciones-notas.dlq");
        p.setColaRiesgoInasistencia("cola-notificaciones-riesgo-inasistencia");
        p.setDlqRiesgoInasistencia("cola-notificaciones-riesgo-inasistencia.dlq");
        p.setColaRiesgoRendimiento("cola-notificaciones-riesgo-rendimiento");
        p.setDlqRiesgoRendimiento("cola-notificaciones-riesgo-rendimiento.dlq");
        p.setColaCambioEstadoAcademico("cola-notificaciones-cambio-estado");
        p.setDlqCambioEstadoAcademico("cola-notificaciones-cambio-estado.dlq");
        p.setPatronEvaluacion("evaluacion.*");
        p.setPatronAsistencia("asistencia.*");
        p.setPatronNota("nota.*");
        p.setClaveEvaluacionCreada("evaluacion.creada");
        p.setClaveEvaluacionActualizada("evaluacion.actualizada");
        p.setClaveEvaluacionEliminada("evaluacion.eliminada");
        p.setClaveAsistenciaRegistrada("asistencia.registrada");
        p.setClaveRiesgoInasistencia("riesgo.inasistencia");
        p.setClaveRiesgoRendimiento("riesgo.rendimiento");
        p.setClaveCambioEstadoAcademico("riesgo.estado-academico");
        p.setClaveNotaCreada("nota.creada");
        p.setClaveNotaActualizada("nota.actualizada");
        return p;
    }
}

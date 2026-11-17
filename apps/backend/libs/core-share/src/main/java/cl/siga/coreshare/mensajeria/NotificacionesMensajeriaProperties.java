package cl.siga.coreshare.mensajeria;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

/**
 * Nombres de la topología RabbitMQ académica (notas, asistencias,
 * evaluaciones y alertas de riesgo). Todos los productores y consumidores
 * comparten esta configuración para no duplicar literales en Java.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "siga.mensajeria.notificaciones")
public class NotificacionesMensajeriaProperties {

    private String exchangeNotificaciones;
    private String exchangeDlq;

    private String colaEvaluaciones;
    private String dlqEvaluaciones;
    private String colaAsistencias;
    private String dlqAsistencias;
    private String colaNotas;
    private String dlqNotas;
    private String colaRiesgoInasistencia;
    private String dlqRiesgoInasistencia;
    private String colaRiesgoRendimiento;
    private String dlqRiesgoRendimiento;
    private String colaCambioEstadoAcademico;
    private String dlqCambioEstadoAcademico;

    private String patronEvaluacion;
    private String patronAsistencia;
    private String patronNota;

    private String claveEvaluacionCreada;
    private String claveEvaluacionActualizada;
    private String claveEvaluacionEliminada;
    private String claveAsistenciaRegistrada;
    private String claveRiesgoInasistencia;
    private String claveRiesgoRendimiento;
    private String claveCambioEstadoAcademico;
    private String claveNotaCreada;
    private String claveNotaActualizada;

    @PostConstruct
    public void validar() {
        requerido(exchangeNotificaciones, "exchange-notificaciones");
        requerido(exchangeDlq, "exchange-dlq");
        requerido(colaEvaluaciones, "cola-evaluaciones");
        requerido(dlqEvaluaciones, "dlq-evaluaciones");
        requerido(colaAsistencias, "cola-asistencias");
        requerido(dlqAsistencias, "dlq-asistencias");
        requerido(colaNotas, "cola-notas");
        requerido(dlqNotas, "dlq-notas");
        requerido(colaRiesgoInasistencia, "cola-riesgo-inasistencia");
        requerido(dlqRiesgoInasistencia, "dlq-riesgo-inasistencia");
        requerido(colaRiesgoRendimiento, "cola-riesgo-rendimiento");
        requerido(dlqRiesgoRendimiento, "dlq-riesgo-rendimiento");
        requerido(colaCambioEstadoAcademico, "cola-cambio-estado-academico");
        requerido(dlqCambioEstadoAcademico, "dlq-cambio-estado-academico");
        requerido(patronEvaluacion, "patron-evaluacion");
        requerido(patronAsistencia, "patron-asistencia");
        requerido(patronNota, "patron-nota");
        requerido(claveEvaluacionCreada, "clave-evaluacion-creada");
        requerido(claveEvaluacionActualizada, "clave-evaluacion-actualizada");
        requerido(claveEvaluacionEliminada, "clave-evaluacion-eliminada");
        requerido(claveAsistenciaRegistrada, "clave-asistencia-registrada");
        requerido(claveRiesgoInasistencia, "clave-riesgo-inasistencia");
        requerido(claveRiesgoRendimiento, "clave-riesgo-rendimiento");
        requerido(claveCambioEstadoAcademico, "clave-cambio-estado-academico");
        requerido(claveNotaCreada, "clave-nota-creada");
        requerido(claveNotaActualizada, "clave-nota-actualizada");
    }

    private static void requerido(String valor, String clave) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                "Falta configurar siga.mensajeria.notificaciones." + clave);
        }
    }
}

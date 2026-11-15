package cl.siga.coreshare.dto.notificaciones;

import cl.siga.coreshare.mensajeria.EventoTrazable;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Alerta RabbitMQ emitida por ms-reportes cuando el rendimiento académico
 * alcanza una condición de riesgo.
 */
public record EventoRiesgoRendimiento(
    @NotNull Long idEstudiante,
    Long idCursoAsignatura,
    @NotNull NivelRiesgo nivelRiesgo,
    @NotNull TipoRiesgo tipoRiesgo,
    Double promedio,
    long evaluacionesInsuficientes,
    String motivo,
    @NotNull LocalDateTime timestamp,
    @NotNull String idEvento,
    @NotNull String correlationId
) implements EventoTrazable {
}

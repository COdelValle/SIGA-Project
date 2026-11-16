package cl.siga.coreshare.dto.notificaciones;

import cl.siga.coreshare.mensajeria.EventoTrazable;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Alerta RabbitMQ emitida por ms-reportes cuando cambia el riesgo de
 * inasistencia o se supera un umbral relevante.
 */
public record EventoRiesgoAsistencia(
    @NotNull Long idEstudiante,
    Long idCursoAsignatura,
    @NotNull NivelRiesgo nivelRiesgo,
    @NotNull TipoRiesgo tipoRiesgo,
    double porcentajeInasistencia,
    String motivo,
    @NotNull LocalDateTime timestamp,
    @NotNull String idEvento,
    @NotNull String correlationId
) implements EventoTrazable {
}

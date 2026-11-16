package cl.siga.coreshare.dto.notificaciones;

import cl.siga.coreshare.mensajeria.EventoTrazable;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Alerta RabbitMQ emitida cuando un estudiante cambia de estado académico.
 */
public record EventoCambioEstadoAcademico(
    @NotNull Long idEstudiante,
    Long idCursoAsignatura,
    @NotNull NivelRiesgo estadoAnterior,
    @NotNull NivelRiesgo estadoNuevo,
    String motivo,
    @NotNull LocalDateTime timestamp,
    @NotNull String idEvento,
    @NotNull String correlationId
) implements EventoTrazable {
}

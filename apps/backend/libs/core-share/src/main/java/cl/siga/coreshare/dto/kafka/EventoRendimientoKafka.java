package cl.siga.coreshare.dto.kafka;

import cl.siga.coreshare.mensajeria.EventoTrazable;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Evento académico publicado en siga.academico.rendimiento.
 * La clave Kafka debe ser el id del estudiante para conservar el orden de sus
 * cambios de rendimiento.
 */
public record EventoRendimientoKafka(
    @NotNull Long idNota,
    @NotNull Long idEstudiante,
    @NotNull Long idEvaluacion,
    Long idCursoAsignatura,
    Double puntaje,
    Double promedio,
    long evaluacionesInsuficientes,
    @NotNull LocalDateTime timestamp,
    @NotNull String idEvento,
    @NotNull String correlationId
) implements EventoTrazable {
}

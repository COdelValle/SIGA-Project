package cl.siga.coreshare.dto.kafka;

import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.mensajeria.EventoTrazable;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Evento académico publicado en siga.academico.asistencias.
 * La clave Kafka debe ser el id del estudiante para conservar el orden de sus
 * registros dentro de una partición.
 */
public record EventoAsistenciaKafka(
    @NotNull Long idAsistencia,
    @NotNull Long idEstudiante,
    @NotNull Long idCursoAsignatura,
    @NotNull LocalDate fecha,
    @NotNull State estado,
    Justificacion justificacion,
    double porcentajeInasistencia,
    long totalRegistros,
    long totalInasistencias,
    @NotNull LocalDateTime timestamp,
    @NotNull String idEvento,
    @NotNull String correlationId
) implements EventoTrazable {
}

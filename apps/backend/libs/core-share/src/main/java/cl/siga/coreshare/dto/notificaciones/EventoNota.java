package cl.siga.coreshare.dto.notificaciones;

import java.time.LocalDateTime;

/**
 * Mensaje que viaja por RabbitMQ cuando se crea o modifica una nota
 * (escala chilena 1.0 a 7.0). Lo produce ms-notas y lo escucha
 * ms-notificaciones (estudiante + apoderados).
 */
public record EventoNota(
    Long idNota,
    Long idEstudiante,
    Long idEvaluacion,
    Double puntaje,
    AccionNota accion,
    LocalDateTime fechaHora
) {
}

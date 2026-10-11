package cl.siga.coreshare.dto.notificaciones;

import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import java.time.LocalDateTime;

/**
 * Mensaje que viaja por RabbitMQ cuando una evaluación se crea,
 * se actualiza o se elimina (borrado lógico).
 * Lo produce ms-evaluaciones y lo escucha ms-notificaciones.
 */
public record EventoEvaluacion(
    Long idEvaluacion,
    String nombre,
    TipoEvaluacion tipo,
    Double ponderacion,
    Long idCursoAsignatura,
    AccionEvaluacion accion,
    LocalDateTime fechaHora,
    String idEvento,
    String nombreAsignatura
) {
}

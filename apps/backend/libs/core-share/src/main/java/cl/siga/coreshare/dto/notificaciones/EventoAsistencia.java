package cl.siga.coreshare.dto.notificaciones;

import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mensaje que viaja por RabbitMQ cuando se registra una asistencia con
 * estado AUSENTE o ATRASADO. Lleva además el porcentaje de inasistencia
 * del estudiante en la dictación durante el mes (faltas AUSENTE / total
 * de registros del mes) y si alcanza el umbral del 60%.
 * Lo produce ms-asistencias y lo escucha ms-notificaciones.
 */
public record EventoAsistencia(
    Long idAsistencia,
    Long idEstudiante,
    Long idCursoAsignatura,
    LocalDate fecha,
    State estado,
    Justificacion justificacion,
    AccionAsistencia accion,
    double porcentajeInasistencia,
    boolean superaUmbralInasistencia,
    LocalDateTime fechaHora
) {
}

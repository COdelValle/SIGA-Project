package cl.siga.coreshare.dto.reportes;

import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record IndicadorAsistenciaDTO(
    Long idEstudiante,
    Long idCursoAsignatura,
    LocalDate fecha,
    State estado,
    double porcentajeInasistencia,
    long totalRegistros,
    long totalInasistencias,
    NivelRiesgo nivelRiesgo,
    LocalDateTime actualizadoEn
) {
}

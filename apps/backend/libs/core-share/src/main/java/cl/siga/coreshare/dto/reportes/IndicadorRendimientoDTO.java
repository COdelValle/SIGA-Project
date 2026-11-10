package cl.siga.coreshare.dto.reportes;

import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import java.time.LocalDateTime;

public record IndicadorRendimientoDTO(
    Long idEstudiante,
    Long idEvaluacion,
    Long idCursoAsignatura,
    Double puntaje,
    Double promedio,
    long evaluacionesInsuficientes,
    NivelRiesgo nivelRiesgo,
    LocalDateTime actualizadoEn
) {
}

package cl.siga.ms_kafka.dto.events.asistencia;


import cl.siga.ms_kafka.dto.events.notificacion.EstadoAsistencia;
import cl.siga.ms_kafka.dto.events.notificacion.NivelRiesgo;

import java.time.LocalDateTime;
import java.util.UUID;

public record AsistenciaEvent(

  UUID idEvento,
  UUID correlationId,

  Long idEstudiante,
  Long idCurso,
  Long idAsignatura,

  LocalDateTime fechaRegistro,

  EstadoAsistencia estado,
  NivelRiesgo nivelRiesgo,

  String registradoPor,
  Integer porcentajeAusencia

) {
}

package cl.siga.ms_kafka.dto.events.asistencia;


import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;

import java.time.LocalDate;
import java.util.UUID;

public record AsistenciaEvent(

  UUID idEvento,
  UUID correlationId,

  Long idAsistencia,
  Long idEstudiante,
  Long idCursoAsignatura,

  LocalDate fecha,

  State estado,
  Justificacion justificacion,

  String observacion,

  String registradoPor

) {}


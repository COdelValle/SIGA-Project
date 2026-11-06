package cl.siga.ms_kafka.processor;

import cl.siga.ms_kafka.service.rules.AsistenciaRules;
import cl.siga.ms_kafka.dto.events.asistencia.AsistenciaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsistenciaProcessor {

  private final AsistenciaRules rules;

  @KafkaListener(
    topics = "siga.academico.asistencias",
    groupId = "reportes-asistencias"
  )
  public void process(AsistenciaEvent event) {

    try {

      rules.obtenerEstado(event.estado());

      rules.calcularRiesgo(
        event.porcentajeAusencia()
      );

      log.info(
        "Evento procesado correctamente {}",
        event.idEvento()
      );

    } catch (IllegalArgumentException ex) {

      log.warn(
        "Datos inválidos para estudiante {}",
        event.idEstudiante()
      );

    } catch (Exception ex) {

      log.error(
        "Error procesando evento {}",
        event.idEvento(),
        ex
      );

      throw ex;
    }
  }
}


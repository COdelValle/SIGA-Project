package cl.siga.ms_kafka.processor;

import cl.siga.ms_kafka.dto.events.enums.NivelRiesgo;
import cl.siga.ms_kafka.model.entity.AsistenciaProjection;
import cl.siga.ms_kafka.service.rules.AsistenciaRules;
import cl.siga.ms_kafka.dto.events.asistencia.AsistenciaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cl.siga.ms_kafka.repository.kafkaRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsistenciaProcessor {

  private final AsistenciaRules rules;
  private final kafkaRepository repository;

  @KafkaListener(
    topics = "siga.academico.asistencias",
    groupId = "reportes-asistencias"
  )
  public void process(
    AsistenciaEvent event
  ) {

    AsistenciaProjection projection =
      repository
        .findByIdEstudiante(
          event.idEstudiante()
        )
        .orElse(new AsistenciaProjection());

    projection.regEstado(event.estado());

    projection.actualizarIndicadores();

    NivelRiesgo riesgo =
      rules.calcularRiesgo(
        projection.getPorcentajeAusencia()
      );

    projection.setNivelRiesgo(riesgo);

    log.info(
      "Estudiante {} - {}% - {}",
      projection.getIdEstudiante(),
      projection.getPorcentajeAusencia(),
      riesgo
    );
  }
}


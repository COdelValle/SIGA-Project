package cl.siga.ms_kafka.producer;

import cl.siga.ms_kafka.dto.events.asistencia.AsistenciaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducer {

  private final KafkaTemplate<String, AsistenciaEvent> kafkaTemplate;

  public void enviar(AsistenciaEvent event) {

    kafkaTemplate.send(
      "siga.academico.asistencias",
      event.idEstudiante().toString(),
      event
    );
  }
}

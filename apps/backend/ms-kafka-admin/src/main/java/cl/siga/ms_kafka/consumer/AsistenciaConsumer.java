package cl.siga.ms_kafka.consumer;


import cl.siga.ms_kafka.dto.events.asistencia.AsistenciaEvent;
import cl.siga.ms_kafka.processor.AsistenciaProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsistenciaConsumer {

  private final AsistenciaProcessor processor;

  @KafkaListener(
    topics = "siga.academico.asistencias",
    groupId = "reportes-asistencias"
  )
  public void consumir(
    AsistenciaEvent event
  ) {

    processor.process(event);
  }
}

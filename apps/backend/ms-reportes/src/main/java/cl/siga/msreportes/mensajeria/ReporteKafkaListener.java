package cl.siga.msreportes.mensajeria;

import cl.siga.coreshare.dto.kafka.EventoAsistenciaKafka;
import cl.siga.coreshare.dto.kafka.EventoRendimientoKafka;
import cl.siga.coreshare.mensajeria.NombresKafka;
import cl.siga.msreportes.service.ReporteProcesamientoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReporteKafkaListener {

    private final ReporteProcesamientoService procesamientoService;

    @KafkaListener(
        topics = NombresKafka.TOPIC_ASISTENCIAS,
        groupId = NombresKafka.GRUPO_REPORTES_ASISTENCIAS,
        containerFactory = "asistenciaKafkaListenerContainerFactory")
    public void recibirAsistencia(ConsumerRecord<String, EventoAsistenciaKafka> record) {
        log.info("Evento Kafka de asistencia recibido topic={} particion={} offset={} idEvento={}",
            record.topic(), record.partition(), record.offset(),
            record.value() == null ? null : record.value().idEvento());
        procesamientoService.procesarAsistencia(record);
    }

    @KafkaListener(
        topics = NombresKafka.TOPIC_RENDIMIENTO,
        groupId = NombresKafka.GRUPO_REPORTES_RENDIMIENTO,
        containerFactory = "rendimientoKafkaListenerContainerFactory")
    public void recibirRendimiento(ConsumerRecord<String, EventoRendimientoKafka> record) {
        log.info("Evento Kafka de rendimiento recibido topic={} particion={} offset={} idEvento={}",
            record.topic(), record.partition(), record.offset(),
            record.value() == null ? null : record.value().idEvento());
        procesamientoService.procesarRendimiento(record);
    }
}

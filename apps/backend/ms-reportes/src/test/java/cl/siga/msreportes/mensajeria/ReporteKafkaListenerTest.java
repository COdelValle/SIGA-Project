package cl.siga.msreportes.mensajeria;

import static org.mockito.Mockito.verify;

import cl.siga.coreshare.dto.kafka.EventoAsistenciaKafka;
import cl.siga.coreshare.dto.kafka.EventoRendimientoKafka;
import cl.siga.msreportes.service.ReporteProcesamientoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReporteKafkaListenerTest {

    @Test
    void delegaEventoDeAsistenciaAlServicioDeProcesamiento() {
        ReporteProcesamientoService service = Mockito.mock(ReporteProcesamientoService.class);
        ReporteKafkaListener listener = new ReporteKafkaListener(service);
        EventoAsistenciaKafka evento = new EventoAsistenciaKafka(
            1L, 10L, 20L, LocalDate.now(),
            cl.siga.coreshare.dto.asistencia.enums.State.AUSENTE, null,
            30.0, 10, 3, LocalDateTime.now(), "evento-a", "corr-a");
        ConsumerRecord<String, EventoAsistenciaKafka> record =
            new ConsumerRecord<>("siga.academico.asistencias", 0, 4L, "10", evento);

        listener.recibirAsistencia(record);

        verify(service).procesarAsistencia(record);
    }

    @Test
    void delegaEventoDeRendimientoAlServicioDeProcesamiento() {
        ReporteProcesamientoService service = Mockito.mock(ReporteProcesamientoService.class);
        ReporteKafkaListener listener = new ReporteKafkaListener(service);
        EventoRendimientoKafka evento = new EventoRendimientoKafka(
            2L, 10L, 30L, 20L, 3.5, null, 2,
            LocalDateTime.now(), "evento-r", "corr-r");
        ConsumerRecord<String, EventoRendimientoKafka> record =
            new ConsumerRecord<>("siga.academico.rendimiento", 1, 5L, "10", evento);

        listener.recibirRendimiento(record);

        verify(service).procesarRendimiento(record);
    }
}

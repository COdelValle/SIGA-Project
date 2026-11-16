package cl.siga.msasistencias.mensajeria;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.dto.kafka.EventoAsistenciaKafka;
import cl.siga.coreshare.mensajeria.NombresKafka;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class PublicadorAsistenciaKafkaTest {

    @Test
    void publicaEnTopicConLaClaveDelEstudiante() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq(NombresKafka.TOPIC_ASISTENCIAS), eq("10"), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        PublicadorAsistenciaKafka publicador = new PublicadorAsistenciaKafka(kafkaTemplate);

        publicador.publicar(new AsistenciaResponseDTO(
            2L, 10L, 4L, LocalDate.of(2026, 10, 7), State.AUSENTE,
            Justificacion.PENDIENTE, null), 60.0, 10L, 6L);

        verify(kafkaTemplate).send(eq(NombresKafka.TOPIC_ASISTENCIAS), eq("10"), any(EventoAsistenciaKafka.class));
    }

    @Test
    void noPropagaFallaSincronaDeKafka() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq(NombresKafka.TOPIC_ASISTENCIAS), eq("10"), any()))
            .thenThrow(new IllegalStateException("broker no disponible"));
        PublicadorAsistenciaKafka publicador = new PublicadorAsistenciaKafka(kafkaTemplate);

        assertThatCode(() -> publicador.publicar(new AsistenciaResponseDTO(
            2L, 10L, 4L, LocalDate.of(2026, 10, 7), State.AUSENTE,
            Justificacion.PENDIENTE, null), 60.0, 10L, 6L)).doesNotThrowAnyException();
    }
}

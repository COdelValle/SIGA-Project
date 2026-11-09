package cl.siga.msnotas.mensajeria;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.dto.kafka.EventoRendimientoKafka;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.mensajeria.NombresKafka;
import cl.siga.msnotas.client.EvaluacionClient;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class PublicadorNotaKafkaTest {

    @Test
    void publicaEnTopicConLaClaveDelEstudiante() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        EvaluacionClient evaluacionClient = mock(EvaluacionClient.class);
        when(evaluacionClient.getEvaluacionById(12L))
            .thenReturn(new EvaluacionResponseDTO(12L, "PRUEBA", TipoEvaluacion.SUMATIVA, 30.0, 8L, true));
        when(kafkaTemplate.send(eq(NombresKafka.TOPIC_RENDIMIENTO), eq("10"), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        PublicadorNotaKafka publicador = new PublicadorNotaKafka(kafkaTemplate, evaluacionClient);

        publicador.publicar(new NotaResponseDTO(4L, 10L, 12L, 6.0));

        verify(kafkaTemplate).send(eq(NombresKafka.TOPIC_RENDIMIENTO), eq("10"), any(EventoRendimientoKafka.class));
    }

    @Test
    void noPropagaFallaSincronaDeKafka() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        EvaluacionClient evaluacionClient = mock(EvaluacionClient.class);
        when(kafkaTemplate.send(eq(NombresKafka.TOPIC_RENDIMIENTO), eq("10"), any()))
            .thenThrow(new IllegalStateException("broker no disponible"));
        PublicadorNotaKafka publicador = new PublicadorNotaKafka(kafkaTemplate, evaluacionClient);

        assertThatCode(() -> publicador.publicar(new NotaResponseDTO(4L, 10L, 12L, 6.0)))
            .doesNotThrowAnyException();
    }
}

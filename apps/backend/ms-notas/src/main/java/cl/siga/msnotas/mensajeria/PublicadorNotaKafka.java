package cl.siga.msnotas.mensajeria;

import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.kafka.EventoRendimientoKafka;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.mensajeria.NombresKafka;
import cl.siga.msnotas.client.EvaluacionClient;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica cambios de notas para que ms-reportes recalcule el rendimiento.
 * La publicación es independiente del productor RabbitMQ existente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicadorNotaKafka {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EvaluacionClient evaluacionClient;

    public void publicar(NotaResponseDTO nota) {
        String idEvento = UUID.randomUUID().toString();
        String correlationId = correlationId();
        Long idCursoAsignatura = idCursoAsignatura(nota);
        EventoRendimientoKafka evento = new EventoRendimientoKafka(
            nota.id(),
            nota.idEstudiante(),
            nota.idEvaluacion(),
            idCursoAsignatura,
            nota.score(),
            null,
            0,
            LocalDateTime.now(),
            idEvento,
            correlationId);

        try {
            kafkaTemplate.send(NombresKafka.TOPIC_RENDIMIENTO,
                String.valueOf(nota.idEstudiante()), evento)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.warn("No se pudo publicar nota en Kafka idEvento={} correlationId={}: {}",
                            idEvento, correlationId, error.getMessage());
                    } else {
                        log.info("Nota publicada en Kafka topic={} idEvento={} correlationId={}",
                            NombresKafka.TOPIC_RENDIMIENTO, idEvento, correlationId);
                    }
                });
        } catch (RuntimeException error) {
            log.warn("Error al iniciar publicación de nota en Kafka idEvento={} correlationId={}: {}",
                idEvento, correlationId, error.getMessage());
        }
    }

    private Long idCursoAsignatura(NotaResponseDTO nota) {
        try {
            EvaluacionResponseDTO evaluacion = evaluacionClient.getEvaluacionById(nota.idEvaluacion());
            return evaluacion == null ? null : evaluacion.idCursoAsignatura();
        } catch (RuntimeException error) {
            log.warn("No se pudo enriquecer nota {} con idCursoAsignatura: {}",
                nota.id(), error.getMessage());
            return null;
        }
    }

    private String correlationId() {
        String value = MDC.get("correlationId");
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }
}

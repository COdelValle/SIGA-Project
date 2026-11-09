package cl.siga.msasistencias.mensajeria;

import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.kafka.EventoAsistenciaKafka;
import cl.siga.coreshare.mensajeria.NombresKafka;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica la representación académica de una asistencia.
 * Kafka es una proyección adicional: una falla de publicación no debe fallar
 * la operación CRUD ni el outbox RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicadorAsistenciaKafka {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publicar(
            AsistenciaResponseDTO asistencia,
            double porcentajeInasistencia,
            long totalRegistros,
            long totalInasistencias) {
        String idEvento = UUID.randomUUID().toString();
        String correlationId = correlationId();
        EventoAsistenciaKafka evento = new EventoAsistenciaKafka(
            asistencia.id(),
            asistencia.idEstudiante(),
            asistencia.idCursoAsignatura(),
            asistencia.fecha(),
            asistencia.estado(),
            asistencia.justificacion(),
            porcentajeInasistencia,
            totalRegistros,
            totalInasistencias,
            LocalDateTime.now(),
            idEvento,
            correlationId);

        try {
            kafkaTemplate.send(NombresKafka.TOPIC_ASISTENCIAS,
                String.valueOf(asistencia.idEstudiante()), evento)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.warn("No se pudo publicar asistencia en Kafka idEvento={} correlationId={}: {}",
                            idEvento, correlationId, error.getMessage());
                    } else {
                        log.info("Asistencia publicada en Kafka topic={} idEvento={} correlationId={}",
                            NombresKafka.TOPIC_ASISTENCIAS, idEvento, correlationId);
                    }
                });
        } catch (RuntimeException error) {
            log.warn("Error al iniciar publicación de asistencia en Kafka idEvento={} correlationId={}: {}",
                idEvento, correlationId, error.getMessage());
        }
    }

    private String correlationId() {
        String value = MDC.get("correlationId");
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }
}

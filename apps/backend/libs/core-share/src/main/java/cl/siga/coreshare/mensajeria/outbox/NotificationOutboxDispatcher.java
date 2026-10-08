package cl.siga.coreshare.mensajeria.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import cl.siga.coreshare.dto.notificaciones.EventoAsistencia;
import cl.siga.coreshare.dto.notificaciones.EventoEvaluacion;
import cl.siga.coreshare.dto.notificaciones.EventoNota;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publica con confirmación los eventos guardados en el outbox del microservicio.
 * Los bloqueos de fila evitan que dos instancias publiquen la misma fila a la vez;
 * idEvento permite al consumidor deduplicar una entrega repetida tras un reinicio.
 */
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxDispatcher {

    private static final int LOTE = 20;
    private static final int MAX_INTENTOS = 8;
    private static final int TIMEOUT_CONFIRMACION_SEGUNDOS = 5;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${siga.notificaciones.outbox.delay-ms:1000}")
    @Transactional
    public void publicarPendientes() {
        List<OutboxRow> pendientes = jdbcTemplate.query("""
            SELECT id_evento, intercambio, clave, tipo_payload, payload, intentos
            FROM notification_event_outbox
            WHERE estado = 'PENDIENTE' AND proximo_intento <= CURRENT_TIMESTAMP(6)
            ORDER BY creado_en
            LIMIT 20
            FOR UPDATE SKIP LOCKED
            """, (rs, rowNum) -> new OutboxRow(
                rs.getString("id_evento"),
                rs.getString("intercambio"),
                rs.getString("clave"),
                rs.getString("tipo_payload"),
                rs.getString("payload"),
                rs.getInt("intentos")));

        for (OutboxRow row : pendientes) {
            despachar(row);
        }
    }

    private void despachar(OutboxRow row) {
        try {
            Object evento = deserializar(row.tipoPayload(), row.payload());
            CorrelationData confirmacion = new CorrelationData(row.idEvento());
            rabbitTemplate.convertAndSend(row.intercambio(), row.clave(), evento, confirmacion);

            CorrelationData.Confirm confirm = confirmacion.getFuture()
                .get(TIMEOUT_CONFIRMACION_SEGUNDOS, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ rechazó la publicación: " + confirm.getReason());
            }
            if (confirmacion.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ no encontró una cola para la clave de enrutamiento.");
            }

            jdbcTemplate.update("""
                UPDATE notification_event_outbox
                SET estado = 'ENVIADO', enviado_en = CURRENT_TIMESTAMP(6), ultimo_error = NULL
                WHERE id_evento = ?
                """, row.idEvento());
            log.info("Evento de notificación confirmado por RabbitMQ idEvento={}", row.idEvento());
        } catch (Exception error) {
            registrarFallo(row, error);
        }
    }

    private Object deserializar(String tipo, String payload) throws Exception {
        return switch (tipo) {
            case "EventoEvaluacion" -> objectMapper.readValue(payload, EventoEvaluacion.class);
            case "EventoNota" -> objectMapper.readValue(payload, EventoNota.class);
            case "EventoAsistencia" -> objectMapper.readValue(payload, EventoAsistencia.class);
            default -> throw new IllegalArgumentException("Tipo de evento de notificación desconocido: " + tipo);
        };
    }

    private void registrarFallo(OutboxRow row, Exception error) {
        int intentos = row.intentos() + 1;
        boolean agotado = intentos >= MAX_INTENTOS;
        int esperaSegundos = Math.min(1 << Math.min(intentos, 8), 300);
        LocalDateTime proximoIntento = agotado
            ? LocalDateTime.now()
            : LocalDateTime.now().plusSeconds(esperaSegundos);
        String estado = agotado ? "FALLIDO" : "PENDIENTE";
        String detalle = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        if (detalle.length() > 1000) {
            detalle = detalle.substring(0, 1000);
        }
        jdbcTemplate.update("""
            UPDATE notification_event_outbox
            SET estado = ?, intentos = ?, proximo_intento = ?, ultimo_error = ?
            WHERE id_evento = ?
            """, estado, intentos, proximoIntento, detalle, row.idEvento());
        log.warn("Fallo al publicar evento de notificación idEvento={} intento={} estado={}",
            row.idEvento(), intentos, estado);
    }

    private record OutboxRow(
        String idEvento,
        String intercambio,
        String clave,
        String tipoPayload,
        String payload,
        int intentos) {
    }
}

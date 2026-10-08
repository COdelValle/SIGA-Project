package cl.siga.coreshare.mensajeria.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Persiste el evento en la misma transacción que el cambio de dominio. */
@RequiredArgsConstructor
public class NotificationEventOutbox {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String idEvento, String intercambio, String clave, Object evento) {
        if (idEvento == null || idEvento.isBlank()) {
            throw new IllegalArgumentException("El evento de notificación debe tener idEvento.");
        }
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(evento);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("No se pudo serializar el evento de notificación.", error);
        }
        LocalDateTime ahora = LocalDateTime.now();

        jdbcTemplate.update("""
            INSERT INTO notification_event_outbox
                (id_evento, intercambio, clave, tipo_payload, payload, estado,
                 intentos, proximo_intento, creado_en)
            VALUES (?, ?, ?, ?, ?, 'PENDIENTE', 0, ?, ?)
            """,
            idEvento,
            intercambio,
            clave,
            evento.getClass().getSimpleName(),
            payload,
            ahora,
            ahora);
    }
}

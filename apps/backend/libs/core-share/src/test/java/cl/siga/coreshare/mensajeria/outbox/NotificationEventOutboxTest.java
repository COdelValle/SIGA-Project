package cl.siga.coreshare.mensajeria.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import cl.siga.coreshare.dto.notificaciones.AccionNota;
import cl.siga.coreshare.dto.notificaciones.EventoNota;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class NotificationEventOutboxTest {

    @Test
    void serializaElEventoEnLaMismaTablaOutboxConSuClaveDeEnrutamiento() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        NotificationEventOutbox outbox = new NotificationEventOutbox(jdbc, new ObjectMapper().findAndRegisterModules());
        EventoNota evento = new EventoNota(7L, 11L, 13L, 6.2, AccionNota.CREADA,
            LocalDateTime.of(2026, 10, 7, 12, 30), "e27e4d75-4fb6-4b06-9e0e-e36ac4c6d041",
            "PRUEBA 1", "Matemática");

        outbox.registrar(evento.idEvento(), "intercambio-notificaciones", "nota.creada", evento);

        verify(jdbc).update(any(String.class),
            eq(evento.idEvento()), eq("intercambio-notificaciones"), eq("nota.creada"),
            eq("EventoNota"), any(String.class), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}

package cl.siga.coreshare.mensajeria.outbox;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.dto.notificaciones.EventoNota;

class NotificationEventOutboxExtraTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void rechazaEventoSinId() {
        NotificationEventOutbox outbox = new NotificationEventOutbox(jdbcTemplate, new ObjectMapper());

        assertThatThrownBy(() -> outbox.registrar(" ", "ex", "rk", new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idEvento");
        assertThatThrownBy(() -> outbox.registrar(null, "ex", "rk", new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idEvento");
    }

    @Test
    void fallaSiElEventoNoSePuedeSerializar() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });
        NotificationEventOutbox outbox = new NotificationEventOutbox(jdbcTemplate, mapper);

        assertThatThrownBy(() -> outbox.registrar("evt-1", "ex", "rk", new EventoNota(
                1L, 2L, 3L, 6.0, null, null, "evt-1", "Prueba", "Matemática")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serializar");
    }
}

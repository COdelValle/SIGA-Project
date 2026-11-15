package cl.siga.coreshare.mensajeria;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.rabbitmq.client.Channel;

class ConfirmadorMensajesErroresTest {

    private ConfirmadorMensajes confirmador() {
        Map<Class<? extends Throwable>, Boolean> recuperables = new HashMap<>();
        recuperables.put(EventoInvalidoException.class, false);
        RetryTemplate plantilla = new RetryTemplate();
        plantilla.setRetryPolicy(new SimpleRetryPolicy(3, recuperables, true, true));
        plantilla.setBackOffPolicy(new NoBackOffPolicy());
        return new ConfirmadorMensajes(plantilla);
    }

    @Test
    void fallaSiNoSePuedeConfirmarElMensaje() throws Exception {
        Channel canal = mock(Channel.class);
        doThrow(new IOException("canal cerrado")).when(canal).basicAck(anyLong(), anyBoolean());

        assertThatThrownBy(() -> confirmador().procesar(canal, 21L, "cola", () -> { }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo confirmar");
    }

    @Test
    void fallaSiNoSePuedeRechazarElMensaje() throws Exception {
        Channel canal = mock(Channel.class);
        doThrow(new IOException("canal cerrado"))
                .when(canal).basicNack(anyLong(), anyBoolean(), anyBoolean());

        assertThatThrownBy(() -> confirmador().procesar(canal, 22L, "cola", () -> {
            throw new EventoInvalidoException("evento invalido");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo rechazar");
    }
}

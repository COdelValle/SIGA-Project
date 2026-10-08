package cl.siga.coreshare.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rabbitmq.client.Channel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

class ConfirmadorMensajesTest {

    private static final String COLA = "cola-notificaciones-notas";

    @Test
    void confirmaConAckCuandoLaOperacionTermina() throws Exception {
        Channel canal = mock(Channel.class);

        confirmador().procesar(canal, 11L, COLA, () -> {
        });

        verify(canal).basicAck(11L, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void rechazaSinReintentarCuandoElEventoEsInvalido() throws Exception {
        Channel canal = mock(Channel.class);
        AtomicInteger intentos = new AtomicInteger();

        confirmador().procesar(canal, 12L, COLA, () -> {
            intentos.incrementAndGet();
            throw new EventoInvalidoException("sin idEstudiante");
        });

        assertThat(intentos.get()).isEqualTo(1);
        verify(canal).basicNack(12L, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void reintentaYConfirmaCuandoElFalloEsTransitorio() throws Exception {
        Channel canal = mock(Channel.class);
        AtomicInteger intentos = new AtomicInteger();

        confirmador().procesar(canal, 13L, COLA, () -> {
            if (intentos.incrementAndGet() < 2) {
                throw new IllegalStateException("base de datos no disponible");
            }
        });

        assertThat(intentos.get()).isEqualTo(2);
        verify(canal).basicAck(13L, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void rechazaHaciaLaDlqCuandoSeAgotanLosReintentos() throws Exception {
        Channel canal = mock(Channel.class);
        AtomicInteger intentos = new AtomicInteger();

        confirmador().procesar(canal, 14L, COLA, () -> {
            intentos.incrementAndGet();
            throw new IllegalStateException("base de datos no disponible");
        });

        assertThat(intentos.get()).isEqualTo(3);
        verify(canal).basicNack(14L, false, false);
        verify(canal, never()).basicAck(14L, false);
    }

    private ConfirmadorMensajes confirmador() {
        Map<Class<? extends Throwable>, Boolean> recuperables = new HashMap<>();
        recuperables.put(EventoInvalidoException.class, false);
        RetryTemplate plantilla = new RetryTemplate();
        plantilla.setRetryPolicy(new SimpleRetryPolicy(3, recuperables, true, true));
        plantilla.setBackOffPolicy(new NoBackOffPolicy());
        return new ConfirmadorMensajes(plantilla);
    }
}

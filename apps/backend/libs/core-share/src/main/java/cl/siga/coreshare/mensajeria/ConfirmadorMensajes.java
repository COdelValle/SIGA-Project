package cl.siga.coreshare.mensajeria;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;

/**
 * Confirma los mensajes consumidos con ACK/NACK explícitos (acknowledge-mode
 * manual). El flujo es siempre el mismo:
 *
 * <ul>
 *   <li>Si el procesamiento termina, se confirma con {@code basicAck}.</li>
 *   <li>Si el mensaje es inválido ({@link EventoInvalidoException}), no se
 *       reintenta y se rechaza con {@code basicNack(requeue=false)} para que
 *       el broker lo enrute a la DLQ.</li>
 *   <li>Si el procesamiento falla por una causa transitoria, se reintenta con
 *       backoff; al agotar los intentos se rechaza hacia la DLQ.</li>
 * </ul>
 *
 * <p>Los reintentos ocurren en memoria (no requeue) para evitar ciclos de
 * redelivery inmediato; el mensaje solo se rechaza definitivamente cuando ya
 * no hay más intentos.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ConfirmadorMensajes {

    private final RetryTemplate retryTemplate;

    /**
     * Procesa la operación del consumidor y confirma o rechaza el mensaje.
     *
     * @param canal     canal AMQP de la entrega (para ack/nack).
     * @param etiqueta  delivery tag del mensaje.
     * @param cola      nombre de la cola de origen (trazabilidad).
     * @param operacion procesamiento del evento, idempotente.
     */
    public void procesar(Channel canal, long etiqueta, String cola, Runnable operacion) {
        try {
            if (!reintentar(operacion)) {
                rechazar(canal, etiqueta, cola, "reintentos agotados");
                return;
            }
            confirmar(canal, etiqueta, cola);
        } catch (EventoInvalidoException invalido) {
            rechazar(canal, etiqueta, cola, invalido.getMessage());
        }
    }

    /**
     * Ejecuta la operación con reintentos acotados. Un {@link EventoInvalidoException}
     * no se reintenta: la política lo clasifica como no recuperable y se propaga
     * de inmediato.
     *
     * @return {@code true} si la operación terminó correctamente.
     */
    private boolean reintentar(Runnable operacion) {
        try {
            retryTemplate.execute(context -> {
                operacion.run();
                return null;
            });
            return true;
        } catch (EventoInvalidoException invalido) {
            throw invalido;
        } catch (RuntimeException transitorio) {
            log.warn("Se agotaron los reintentos de mensajería: {}", transitorio.getMessage());
            return false;
        }
    }

    private void confirmar(Channel canal, long etiqueta, String cola) {
        try {
            canal.basicAck(etiqueta, false);
            log.info("Mensaje confirmado con ACK en cola={} deliveryTag={}", cola, etiqueta);
        } catch (IOException error) {
            throw new IllegalStateException(
                "No se pudo confirmar (ACK) el mensaje de la cola " + cola + ".", error);
        }
    }

    private void rechazar(Channel canal, long etiqueta, String cola, String motivo) {
        try {
            canal.basicNack(etiqueta, false, false);
            log.error("Mensaje rechazado con NACK hacia la DLQ en cola={} deliveryTag={} motivo={}",
                cola, etiqueta, motivo);
        } catch (IOException error) {
            throw new IllegalStateException(
                "No se pudo rechazar (NACK) el mensaje de la cola " + cola + ".", error);
        }
    }
}

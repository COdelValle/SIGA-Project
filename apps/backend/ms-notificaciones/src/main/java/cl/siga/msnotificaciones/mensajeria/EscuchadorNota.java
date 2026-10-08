package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoNota;
import cl.siga.coreshare.mensajeria.ConfirmadorMensajes;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.msnotificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola-notificaciones-notas.
 * Persiste la notificación para el estudiante dueño de la nota; los apoderados
 * vinculados consultan la misma notificación con su propio estado de lectura.
 *
 * <p>La confirmación es manual: ACK cuando la notificación queda persistida
 * (o ya existía, por idempotencia) y NACK hacia la DLQ si el evento es inválido
 * o se agotan los reintentos.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EscuchadorNota {

    private final NotificacionService notificacionService;
    private final ConfirmadorMensajes confirmador;

    @RabbitListener(queues = NombresMensajeria.COLA_NOTAS)
    public void alRecibirEvento(EventoNota evento, Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        log.info("Evento de nota recibido idEvento={} id={}", evento.idEvento(), evento.idNota());
        confirmador.procesar(canal, etiqueta, NombresMensajeria.COLA_NOTAS,
            () -> notificacionService.registrarEvento(evento));
    }
}

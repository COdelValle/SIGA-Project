package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoAsistencia;
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
 * Consumidor de la cola-notificaciones-asistencias.
 * Persiste la notificación para el estudiante asociado; la bandeja del apoderado
 * la comparte mediante el vínculo autorizado del perfil.
 *
 * <p>La confirmación es manual: ACK cuando la notificación queda persistida y
 * NACK hacia la DLQ si el evento es inválido o se agotan los reintentos.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EscuchadorAsistencia {

    private final NotificacionService notificacionService;
    private final ConfirmadorMensajes confirmador;

    @RabbitListener(queues = NombresMensajeria.COLA_ASISTENCIAS)
    public void alRecibirEvento(EventoAsistencia evento, Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        log.info("Evento de asistencia recibido idEvento={} id={}", evento.idEvento(), evento.idAsistencia());
        confirmador.procesar(canal, etiqueta, NombresMensajeria.COLA_ASISTENCIAS,
            () -> notificacionService.registrarEvento(evento));
    }
}

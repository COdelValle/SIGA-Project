package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoEvaluacion;
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
 * Consumidor de la cola-notificaciones-evaluaciones (claves
 * evaluacion.creada/actualizada/eliminada).
 *
 * <p>Persiste una notificación dirigida a la dictación. Al consultar la bandeja,
 * los clientes Feign propagan el JWT del estudiante/apoderado para comprobar la
 * matrícula y los vínculos autorizados.</p>
 *
 * <p>La confirmación es manual: ACK cuando la notificación queda persistida y
 * NACK hacia la DLQ si el evento es inválido o se agotan los reintentos.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EscuchadorEvaluacion {

    private final NotificacionService notificacionService;
    private final ConfirmadorMensajes confirmador;

    @RabbitListener(queues = NombresMensajeria.COLA_EVALUACIONES)
    public void alRecibirEvento(EventoEvaluacion evento, Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        log.info("Evento de evaluación recibido idEvento={} id={}", evento.idEvento(), evento.idEvaluacion());
        confirmador.procesar(canal, etiqueta, NombresMensajeria.COLA_EVALUACIONES,
            () -> notificacionService.registrarEvento(evento));
    }
}

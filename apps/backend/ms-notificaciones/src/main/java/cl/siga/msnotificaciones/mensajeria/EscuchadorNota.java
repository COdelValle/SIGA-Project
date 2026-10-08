package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoNota;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.msnotificaciones.service.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola-notificaciones-notas.
 * Persiste la notificación para el estudiante dueño de la nota; los apoderados
 * vinculados consultan la misma notificación con su propio estado de lectura.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EscuchadorNota {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = NombresMensajeria.COLA_NOTAS)
    public void alRecibirEvento(EventoNota evento) {
        notificacionService.registrarEvento(evento);
        log.info("Evento de nota procesado idEvento={} id={}", evento.idEvento(), evento.idNota());
    }
}

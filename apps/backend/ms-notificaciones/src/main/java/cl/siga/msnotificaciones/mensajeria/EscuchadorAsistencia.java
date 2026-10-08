package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoAsistencia;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.msnotificaciones.service.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola-notificaciones-asistencias.
 * Persiste la notificación para el estudiante asociado; la bandeja del apoderado
 * la comparte mediante el vínculo autorizado del perfil.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EscuchadorAsistencia {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = NombresMensajeria.COLA_ASISTENCIAS)
    public void alRecibirEvento(EventoAsistencia evento) {
        notificacionService.registrarEvento(evento);
        log.info("Evento de asistencia procesado idEvento={} id={}", evento.idEvento(), evento.idAsistencia());
    }
}

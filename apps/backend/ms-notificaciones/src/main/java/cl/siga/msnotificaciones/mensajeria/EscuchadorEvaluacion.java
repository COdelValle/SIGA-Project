package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoEvaluacion;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.msnotificaciones.service.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * El Consumidor: recibe lo que ms-evaluaciones publica en la cola-notificaciones-evaluaciones (claves evaluacion.creada/actualizada/eliminada).
 *
 * Persiste una notificación dirigida a la dictación. Al consultar la bandeja,
 * los clientes Feign propagan el JWT del estudiante/apoderado para comprobar la
 * matrícula y los vínculos autorizados.
 *
 * Si falla la persistencia, la excepción se propaga para reintentos acotados y
 * envío a la DLQ declarada para esta cola.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EscuchadorEvaluacion {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = NombresMensajeria.COLA_EVALUACIONES)
    public void alRecibirEvento(EventoEvaluacion evento) {
        notificacionService.registrarEvento(evento);
        log.info("Evento de evaluación procesado idEvento={} id={}", evento.idEvento(), evento.idEvaluacion());
    }
}

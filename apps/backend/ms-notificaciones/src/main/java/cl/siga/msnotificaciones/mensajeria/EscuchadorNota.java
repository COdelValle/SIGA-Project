package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoNota;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola-notificaciones-notas.
 * Por ahora solo registra en log, como en las colas 1 y 2;
 * los destinatarios son el estudiante dueño de la nota y sus apoderados.
 */
@Component
@Slf4j
public class EscuchadorNota {

    @RabbitListener(queues = NombresMensajeria.COLA_NOTAS)
    public void alRecibirEvento(EventoNota evento) {
        log.info("Notificación de nota recibida -> id={} idEstudiante={} idEvaluacion={} "
                        + "puntaje={} accion={} fechaHora={}",
                evento.idNota(),
                evento.idEstudiante(),
                evento.idEvaluacion(),
                evento.puntaje(),
                evento.accion(),
                evento.fechaHora());
    }
}

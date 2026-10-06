package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoEvaluacion;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * El Consumidor: recibe lo que ms-evaluaciones publica en la cola-notificaciones-evaluaciones (claves evaluacion.creada/actualizada/eliminada).
 *
 * Por ahora solo registra el evento en log, como se acordó. Los clientes Feign
 * (ClienteAsignatura, ClienteInscripcion, ClienteEstudiante, ClienteApoderado)
 * ya están creados para cuando se resuelva la autenticación servicio-a-servicio
 * y se pueda enriquecer el aviso con nombres de estudiantes y apoderados.
 *
 * Si este método lanza una excepción, RabbitMQ reintenta el mensaje; por eso
 * aquí NO se lanza nada: un evento malo no debe trabar la cola.
 */
@Component
@Slf4j
public class EscuchadorEvaluacion {

    @RabbitListener(queues = NombresMensajeria.COLA_EVALUACIONES)
    public void alRecibirEvento(EventoEvaluacion evento) {
        log.info("Notificación de evaluación recibida -> accion={} id={} nombre={} tipo={} "
                        + "ponderacion={} idCursoAsignatura={} fechaHora={}",
                evento.accion(),
                evento.idEvaluacion(),
                evento.nombre(),
                evento.tipo(),
                evento.ponderacion(),
                evento.idCursoAsignatura(),
                evento.fechaHora());
    }
}

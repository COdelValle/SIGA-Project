package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoAsistencia;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola-notificaciones-asistencias.
 * Igual que el de evaluaciones: por ahora solo registra en log, como se acordó.
 * Incluye el porcentaje de inasistencia del mes y si supera el umbral del 60%.
 */
@Component
@Slf4j
public class EscuchadorAsistencia {

    @RabbitListener(queues = NombresMensajeria.COLA_ASISTENCIAS)
    public void alRecibirEvento(EventoAsistencia evento) {
        log.info("Notificación de asistencia recibida -> id={} idEstudiante={} idCursoAsignatura={} "
                        + "fecha={} estado={} accion={} porcentajeInasistencia={} superaUmbral={} fechaHora={}",
                evento.idAsistencia(),
                evento.idEstudiante(),
                evento.idCursoAsignatura(),
                evento.fecha(),
                evento.estado(),
                evento.accion(),
                evento.porcentajeInasistencia(),
                evento.superaUmbralInasistencia(),
                evento.fechaHora());
    }
}

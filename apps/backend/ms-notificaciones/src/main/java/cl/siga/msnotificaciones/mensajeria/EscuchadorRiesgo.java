package cl.siga.msnotificaciones.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoCambioEstadoAcademico;
import cl.siga.coreshare.dto.notificaciones.EventoRiesgoAsistencia;
import cl.siga.coreshare.dto.notificaciones.EventoRiesgoRendimiento;
import cl.siga.coreshare.mensajeria.ConfirmadorMensajes;
import cl.siga.coreshare.mensajeria.NotificacionesMensajeriaProperties;
import cl.siga.msnotificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EscuchadorRiesgo {

    private final NotificacionService notificacionService;
    private final ConfirmadorMensajes confirmador;
    private final NotificacionesMensajeriaProperties propiedades;

    @RabbitListener(queues = "${siga.mensajeria.notificaciones.cola-riesgo-inasistencia}")
    public void recibirInasistencia(EventoRiesgoAsistencia evento, Channel canal,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        procesar(canal, etiqueta, propiedades.getColaRiesgoInasistencia(),
            evento.idEvento(), () -> notificacionService.registrarEvento(evento));
    }

    @RabbitListener(queues = "${siga.mensajeria.notificaciones.cola-riesgo-rendimiento}")
    public void recibirRendimiento(EventoRiesgoRendimiento evento, Channel canal,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        procesar(canal, etiqueta, propiedades.getColaRiesgoRendimiento(),
            evento.idEvento(), () -> notificacionService.registrarEvento(evento));
    }

    @RabbitListener(queues = "${siga.mensajeria.notificaciones.cola-cambio-estado-academico}")
    public void recibirCambioEstado(EventoCambioEstadoAcademico evento, Channel canal,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        procesar(canal, etiqueta, propiedades.getColaCambioEstadoAcademico(),
            evento.idEvento(), () -> notificacionService.registrarEvento(evento));
    }

    private void procesar(Channel canal, long etiqueta, String cola, String idEvento, Runnable accion) {
        log.info("Evento de riesgo recibido idEvento={} cola={}", idEvento, cola);
        confirmador.procesar(canal, etiqueta, cola, accion);
    }
}

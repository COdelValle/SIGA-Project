package cl.siga.msreportes.mensajeria;

import cl.siga.coreshare.dto.notificaciones.EventoCambioEstadoAcademico;
import cl.siga.coreshare.dto.notificaciones.EventoRiesgoAsistencia;
import cl.siga.coreshare.dto.notificaciones.EventoRiesgoRendimiento;
import cl.siga.coreshare.mensajeria.NotificacionesMensajeriaProperties;
import cl.siga.coreshare.mensajeria.outbox.NotificationEventOutbox;
import cl.siga.msreportes.model.entity.AlertaRiesgo;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicadorAlertaRiesgo {

    private final NotificationEventOutbox outbox;
    private final NotificacionesMensajeriaProperties propiedades;

    public void publicar(AlertaRiesgo alerta) {
        String idEvento = UUID.nameUUIDFromBytes(
            alerta.getClaveIdempotencia().getBytes(StandardCharsets.UTF_8)).toString();
        LocalDateTime timestamp = alerta.getCreadaEn();
        switch (alerta.getTipoRiesgo()) {
            case INASISTENCIA -> outbox.registrar(idEvento, propiedades.getExchangeNotificaciones(),
                propiedades.getClaveRiesgoInasistencia(),
                new EventoRiesgoAsistencia(alerta.getIdEstudiante(), alerta.getIdCursoAsignatura(),
                    alerta.getNivelNuevo(), alerta.getTipoRiesgo(),
                    alerta.getPorcentajeInasistencia() == null ? 0.0 : alerta.getPorcentajeInasistencia(),
                    alerta.getMotivo(),
                    timestamp, idEvento, alerta.getCorrelationId()));
            case BAJO_RENDIMIENTO -> outbox.registrar(idEvento, propiedades.getExchangeNotificaciones(),
                propiedades.getClaveRiesgoRendimiento(),
                new EventoRiesgoRendimiento(alerta.getIdEstudiante(), alerta.getIdCursoAsignatura(),
                    alerta.getNivelNuevo(), alerta.getTipoRiesgo(), alerta.getPromedio(),
                    alerta.getEvaluacionesInsuficientes() == null ? 0 : alerta.getEvaluacionesInsuficientes(),
                    alerta.getMotivo(), timestamp, idEvento, alerta.getCorrelationId()));
            case CAMBIO_ESTADO_ACADEMICO -> outbox.registrar(idEvento, propiedades.getExchangeNotificaciones(),
                propiedades.getClaveCambioEstadoAcademico(),
                new EventoCambioEstadoAcademico(alerta.getIdEstudiante(), alerta.getIdCursoAsignatura(),
                    alerta.getNivelAnterior(), alerta.getNivelNuevo(), alerta.getMotivo(),
                    timestamp, idEvento, alerta.getCorrelationId()));
        }
    }
}

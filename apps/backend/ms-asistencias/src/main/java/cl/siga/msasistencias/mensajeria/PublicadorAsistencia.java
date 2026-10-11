package cl.siga.msasistencias.mensajeria;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.notificaciones.AccionAsistencia;
import cl.siga.coreshare.dto.notificaciones.EventoAsistencia;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.coreshare.mensajeria.outbox.NotificationEventOutbox;
import cl.siga.msasistencias.client.AsignaturaClient;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * El Productor de asistencias vive en ms-asistencias.
 * Solo se llama al crear (POST) con estado AUSENTE o ATRASADO; el mensaje
 * lleva el porcentaje de inasistencia del mes, si supera el umbral del 60% y
 * el nombre de la asignatura cuando el servicio responde.
 * Persiste el evento en el outbox de la misma transacción del CRUD.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicadorAsistencia {

    private final NotificationEventOutbox outbox;
    private final AsignaturaClient asignaturaClient;

    /** Se llama al registrar (POST /api/v1/asistencias) si es AUSENTE o ATRASADO. */
    public void publicarRegistrada(AsistenciaResponseDTO asistencia,
                                   double porcentajeInasistencia,
                                   boolean superaUmbral) {
        EventoAsistencia evento = new EventoAsistencia(
                asistencia.id(),
                asistencia.idEstudiante(),
                asistencia.idCursoAsignatura(),
                asistencia.fecha(),
                asistencia.estado(),
                asistencia.justificacion(),
                AccionAsistencia.REGISTRADA,
                porcentajeInasistencia,
                superaUmbral,
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
                nombreAsignatura(asistencia)
        );
        outbox.registrar(evento.idEvento(), NombresMensajeria.INTERCAMBIO_NOTIFICACIONES,
                NombresMensajeria.CLAVE_ASISTENCIA_REGISTRADA, evento);
        log.info("Evento de asistencia agregado al outbox: id={} estado={} superaUmbral={}",
                asistencia.id(), asistencia.estado(), superaUmbral);
    }

    private String nombreAsignatura(AsistenciaResponseDTO asistencia) {
        if (asistencia.idCursoAsignatura() == null) {
            return null;
        }
        try {
            CursoAsignaturaResponseDTO dictacion =
                    asignaturaClient.getCursoAsignaturaById(asistencia.idCursoAsignatura());
            return dictacion == null ? null : dictacion.nombre();
        } catch (Exception error) {
            log.warn("No se pudo obtener el nombre de la asignatura de la dictación {}: {}",
                    asistencia.idCursoAsignatura(), error.getMessage());
            return null;
        }
    }
}

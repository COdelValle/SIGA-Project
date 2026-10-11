package cl.siga.msevaluaciones.mensajeria;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.notificaciones.AccionEvaluacion;
import cl.siga.coreshare.dto.notificaciones.EventoEvaluacion;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.coreshare.mensajeria.outbox.NotificationEventOutbox;
import cl.siga.msevaluaciones.client.AsignaturaClient;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * El Productor de evaluaciones vive en ms-evaluaciones.
 * Después de guardar en la base, agrega el evento al outbox de la misma
 * transacción, enriquecido con el nombre de la asignatura cuando el servicio
 * responde; si falla, publica igual con texto de respaldo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicadorEvaluacion {

    private final NotificationEventOutbox outbox;
    private final AsignaturaClient asignaturaClient;

    /** Se llama al crear (POST /api/v1/evaluaciones). */
    public void publicarCreada(EvaluacionResponseDTO evaluacion) {
        publicar(evaluacion, AccionEvaluacion.CREADA, NombresMensajeria.CLAVE_EVALUACION_CREADA);
    }

    /** Se llama al modificar (PUT /api/v1/evaluaciones/{id}). */
    public void publicarActualizada(EvaluacionResponseDTO evaluacion) {
        publicar(evaluacion, AccionEvaluacion.ACTUALIZADA, NombresMensajeria.CLAVE_EVALUACION_ACTUALIZADA);
    }

    /** Se llama al eliminar (DELETE lógico). */
    public void publicarEliminada(EvaluacionResponseDTO evaluacion) {
        publicar(evaluacion, AccionEvaluacion.ELIMINADA, NombresMensajeria.CLAVE_EVALUACION_ELIMINADA);
    }

    private void publicar(EvaluacionResponseDTO evaluacion, AccionEvaluacion accion, String claveEnrutamiento) {
        EventoEvaluacion evento = new EventoEvaluacion(
                evaluacion.id(),
                evaluacion.nombre(),
                evaluacion.tipo(),
                evaluacion.ponderacion(),
                evaluacion.idCursoAsignatura(),
                accion,
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
                nombreAsignatura(evaluacion)
        );
        outbox.registrar(evento.idEvento(), NombresMensajeria.INTERCAMBIO_NOTIFICACIONES, claveEnrutamiento, evento);
        log.info("Evento de evaluación agregado al outbox: accion={} id={} clave={}",
                accion, evaluacion.id(), claveEnrutamiento);
    }

    private String nombreAsignatura(EvaluacionResponseDTO evaluacion) {
        if (evaluacion.idCursoAsignatura() == null) {
            return null;
        }
        try {
            CursoAsignaturaResponseDTO dictacion =
                    asignaturaClient.getCursoAsignaturaById(evaluacion.idCursoAsignatura());
            return dictacion == null ? null : dictacion.nombre();
        } catch (Exception error) {
            log.warn("No se pudo obtener el nombre de la asignatura de la dictación {}: {}",
                    evaluacion.idCursoAsignatura(), error.getMessage());
            return null;
        }
    }
}

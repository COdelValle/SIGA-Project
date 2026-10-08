package cl.siga.msnotas.mensajeria;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notificaciones.AccionNota;
import cl.siga.coreshare.dto.notificaciones.EventoNota;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.coreshare.mensajeria.outbox.NotificationEventOutbox;
import cl.siga.msnotas.client.AsignaturaClient;
import cl.siga.msnotas.client.EvaluacionClient;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * El Productor de notas vive en ms-notas.
 * Se llama al crear (POST, fila nueva o reactivación) y al modificar (PUT).
 * Persiste el evento en el outbox de la misma transacción del CRUD y agrega
 * el nombre de la evaluación/asignatura cuando los servicios responden; si el
 * enriquecimiento falla, el aviso igual se emite (el consumidor usa un texto
 * genérico de respaldo).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicadorNota {

    private final NotificationEventOutbox outbox;
    private final EvaluacionClient evaluacionClient;
    private final AsignaturaClient asignaturaClient;

    /** POST con fila nueva. */
    public void publicarCreada(NotaResponseDTO nota) {
        publicar(nota, AccionNota.CREADA, NombresMensajeria.CLAVE_NOTA_CREADA);
    }

    /** PUT, o POST que reactiva una nota borrada lógicamente. */
    public void publicarActualizada(NotaResponseDTO nota) {
        publicar(nota, AccionNota.ACTUALIZADA, NombresMensajeria.CLAVE_NOTA_ACTUALIZADA);
    }

    private void publicar(NotaResponseDTO nota, AccionNota accion, String claveEnrutamiento) {
        EventoNota evento = new EventoNota(
                nota.id(),
                nota.idEstudiante(),
                nota.idEvaluacion(),
                nota.score(),
                accion,
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
                nombreEvaluacion(nota),
                nombreAsignatura(nota)
        );
        outbox.registrar(evento.idEvento(), NombresMensajeria.INTERCAMBIO_NOTIFICACIONES, claveEnrutamiento, evento);
        log.info("Evento de nota agregado al outbox: accion={} id={} clave={}",
                accion, nota.id(), claveEnrutamiento);
    }

    private String nombreEvaluacion(NotaResponseDTO nota) {
        if (nota.idEvaluacion() == null) {
            return null;
        }
        try {
            EvaluacionResponseDTO evaluacion = evaluacionClient.getEvaluacionById(nota.idEvaluacion());
            return evaluacion == null ? null : evaluacion.nombre();
        } catch (Exception error) {
            log.warn("No se pudo obtener el nombre de la evaluación {}: {}",
                    nota.idEvaluacion(), error.getMessage());
            return null;
        }
    }

    private String nombreAsignatura(NotaResponseDTO nota) {
        if (nota.idEvaluacion() == null) {
            return null;
        }
        try {
            EvaluacionResponseDTO evaluacion = evaluacionClient.getEvaluacionById(nota.idEvaluacion());
            if (evaluacion == null || evaluacion.idCursoAsignatura() == null) {
                return null;
            }
            CursoAsignaturaResponseDTO dictacion =
                    asignaturaClient.getCursoAsignaturaById(evaluacion.idCursoAsignatura());
            return dictacion == null ? null : dictacion.nombre();
        } catch (Exception error) {
            log.warn("No se pudo obtener el nombre de la asignatura de la evaluación {}: {}",
                    nota.idEvaluacion(), error.getMessage());
            return null;
        }
    }
}

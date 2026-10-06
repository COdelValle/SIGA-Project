package cl.siga.msevaluaciones.mensajeria;

import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.notificaciones.AccionEvaluacion;
import cl.siga.coreshare.dto.notificaciones.EventoEvaluacion;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * El Productor de tu imagen vive en ms-evaluaciones.
 * Después de guardar en la base, publica un EventoEvaluacion al
 * intercambio-notificaciones con su clave (creada/actualizada/eliminada).
 * Si RabbitMQ está caído, solo loguea y NO rompe el CRUD (fire-and-forget).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicadorEvaluacion {

    private final RabbitTemplate plantillaConejo;

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
                LocalDateTime.now()
        );
        try {
            plantillaConejo.convertAndSend(
                    NombresMensajeria.INTERCAMBIO_NOTIFICACIONES,
                    claveEnrutamiento,
                    evento
            );
            log.info("Evento de evaluación publicado: accion={} id={} clave={}",
                    accion, evaluacion.id(), claveEnrutamiento);
        } catch (Exception error) {
            // La evaluación ya quedó guardada; la notificación es secundaria.
            log.warn("No se pudo publicar el evento de evaluación id={} accion={}: {}",
                    evaluacion.id(), accion, error.getMessage());
        }
    }
}

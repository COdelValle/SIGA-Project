package cl.siga.coreshare.dto.notificaciones;

/**
 * Qué le pasó a la evaluación.
 * CREADA = se registró una nueva.
 * ACTUALIZADA = cambió nombre, tipo o ponderación.
 * ELIMINADA = borrado lógico (active = false).
 */
public enum AccionEvaluacion {
    CREADA,
    ACTUALIZADA,
    ELIMINADA
}

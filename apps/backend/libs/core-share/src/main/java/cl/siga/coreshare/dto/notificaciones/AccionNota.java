package cl.siga.coreshare.dto.notificaciones;

/**
 * Qué le pasó a la nota.
 * CREADA = fila nueva en el POST.
 * ACTUALIZADA = PUT, o POST que reactiva una nota borrada lógicamente
 * (se reutiliza la misma fila).
 */
public enum AccionNota {
    CREADA,
    ACTUALIZADA
}

package cl.siga.coreshare.dto.notificaciones;

import java.time.LocalDateTime;

/** Representación segura de una notificación dentro de la bandeja de SIGA. */
public record NotificacionResponseDTO(
    Long id,
    TipoNotificacion tipo,
    String accion,
    String titulo,
    String resumen,
    LocalDateTime fechaHora,
    boolean leida
) {
}

package cl.siga.coreshare.dto.usuario;

import java.time.OffsetDateTime;

import cl.siga.coreshare.dto.usuario.enums.Rol;

/**
 * Aviso para el futuro servicio de notificaciones (opción 3: entrega de la
 * clave temporal por correo). No incluye la contraseña: solo anuncia que la
 * cuenta quedó aprovisionada y con qué correo de contacto avisar. El servicio
 * de notificaciones deberá obtener la credencial por un canal seguro y de un
 * solo uso cuando se implemente.
 *
 * <p>Se emite únicamente si {@code REGISTRO_NOTIFY_CREDENTIALS_ENABLED=true}.</p>
 */
public record UserCredentialsNotificationEventDTO(
    String schemaVersion,
    String eventId,
    String processId,
    String correlationId,
    String email,
    String contactEmail,
    String userId,
    Rol requestedRole,
    OffsetDateTime occurredAt
) {
    public static final String CURRENT_SCHEMA_VERSION = "v1";
}

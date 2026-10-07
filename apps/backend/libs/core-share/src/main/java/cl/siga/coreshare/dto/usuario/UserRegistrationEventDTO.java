package cl.siga.coreshare.dto.usuario;

import java.time.OffsetDateTime;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO;

/**
 * Evento canónico del registro asíncrono.
 *
 * <p>Se publica dos veces para un mismo proceso:</p>
 * <ol>
 *   <li>Con {@code userId} nulo hacia la etapa de Entra ID (routing key
 *       {@code user.registration.azure}).</li>
 *   <li>Con {@code userId} ya resuelto hacia la cola del microservicio del rol
 *       (routing keys {@code user.register.*}).</li>
 * </ol>
 *
 * <p>Nunca incluye credenciales ni datos de contacto: la contraseña temporal se
 * cifra y se entrega una única vez por el endpoint de credencial.</p>
 */
public record UserRegistrationEventDTO(
    String schemaVersion,
    String eventId,
    String processId,
    String correlationId,
    String email,
    String fullName,
    Rol requestedRole,
    String userId,
    DatosRegistroRolDTO roleData,
    OffsetDateTime occurredAt
) {
    public static final String CURRENT_SCHEMA_VERSION = "v1";

    public boolean schemaSoportado() {
        return CURRENT_SCHEMA_VERSION.equals(schemaVersion);
    }
}

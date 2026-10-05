package cl.siga.coreshare.dto.usuario;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;

import cl.siga.coreshare.dto.usuario.enums.Rol;

public record UserRegistrationEventDTO(
    String schemaVersion,
    String eventId,
    String processId,
    String correlationId,
    String email,
    String fullName,
    Rol requestedRole,
    String userId,
    Map<String, Object> roleData,
    OffsetDateTime occurredAt
) implements Serializable {
}

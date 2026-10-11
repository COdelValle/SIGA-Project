package cl.siga.coreshare.dto.usuario;

import java.io.Serializable;
import java.time.OffsetDateTime;

import cl.siga.coreshare.dto.usuario.enums.RegistrationStepType;

public record UserRegistrationStepResultEventDTO(
    String processId,
    String eventId,
    RegistrationStepType stepType,
    boolean success,
    String message,
    OffsetDateTime occurredAt
) implements Serializable {
}

package cl.siga.coreshare.dto.usuario;

import java.time.OffsetDateTime;

import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepState;
import cl.siga.coreshare.dto.usuario.enums.Rol;

public record UserRegistrationStatusResponseDTO(
    String processId,
    RegistrationProcessState state,
    RegistrationStepState azureState,
    RegistrationStepState domainState,
    Rol requestedRole,
    String email,
    String userId,
    String errorMessage,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}

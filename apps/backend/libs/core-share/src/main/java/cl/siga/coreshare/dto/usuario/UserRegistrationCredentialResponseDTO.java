package cl.siga.coreshare.dto.usuario;

import java.time.OffsetDateTime;

/**
 * Credencial temporal de Entra ID para un registro asíncrono. Se entrega una
 * sola vez al administrador que inició el proceso (o como respuesta de un
 * reset síncrono) y nunca se persiste en claro.
 */
public record UserRegistrationCredentialResponseDTO(
    String processId,
    String email,
    String userId,
    String temporaryPassword,
    OffsetDateTime expiresAt
) {
}

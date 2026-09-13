package cl.siga.coreshare.dto.usuario;

/**
 * Resultado de buscar una cuenta en Entra ID (Microsoft Graph) antes de
 * registrarla localmente. El {@code oid} es el object id que se usa como ID.
 */
public record CandidatoUsuarioResponseDTO(
    String oid,
    String email,
    String displayName
) {
}

package cl.siga.coreshare.dto.usuario;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;

public record UsuarioResponseDTO(
    String id,
    String email,
    Rol rol,
    StateUsuario state
) {
}

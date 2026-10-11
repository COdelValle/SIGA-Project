package cl.siga.coreshare.dto.usuario;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;

/**
 * Resultado del alta de un usuario: {@code ACTIVO} cuando se registró con su
 * {@code oid} (resuelto por Graph o entregado explícitamente), o
 * {@code INVITADO} cuando quedó pendiente de vincularse en el primer login.
 */
public record RegistrarUsuarioResponseDTO(
    String id,
    String email,
    Rol rol,
    StateUsuario state
) {
}

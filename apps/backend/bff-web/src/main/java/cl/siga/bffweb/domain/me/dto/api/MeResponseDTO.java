package cl.siga.bffweb.domain.me.dto.api;

import java.util.List;

import cl.siga.coreshare.dto.usuario.enums.Rol;

/**
 * Perfil del usuario autenticado que consume el frontend para decidir el portal.
 */
public record MeResponseDTO(
    String id,
    String email,
    String displayName,
    List<Rol> roles
) {
}

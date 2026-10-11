package cl.siga.bffweb.domain.me;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.bffweb.domain.me.dto.api.MeResponseDTO;
import cl.siga.bffweb.integration.usuarios.UsuarioClient;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import cl.siga.coreshare.security.SecurityUtils;
import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class MeService {
    private final UsuarioClient usuarioClient;

    @Transactional (readOnly = true)
    public MeResponseDTO getMe() {
        UsuarioResponseDTO usuario;
        try {
            usuario = usuarioClient.getCurrentUsuario();
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("El usuario autenticado no está registrado en SIGA.");
        } catch (FeignException ex) {
            throw new ServiceUnavailableException(
                    "No se pudo obtener el usuario autenticado (HTTP " + ex.status() + ").");
        }

        // El nombre estandarizado del registro manda; si no existe (cuenta legacy),
        // se usa el claim del token y, como ultimo recurso, el correo.
        String displayName = (usuario.fullName() != null && !usuario.fullName().isBlank())
                ? usuario.fullName()
                : SecurityUtils.getCurrentJwt()
                        .map(jwt -> jwt.getClaimAsString("name"))
                        .filter(name -> name != null && !name.isBlank())
                        .orElse(usuario.email());

        return new MeResponseDTO(usuario.id(), usuario.email(), displayName, List.of(usuario.rol()));
    }
}

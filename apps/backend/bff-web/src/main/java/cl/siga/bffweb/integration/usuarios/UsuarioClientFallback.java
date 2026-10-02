package cl.siga.bffweb.integration.usuarios;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component 
public class UsuarioClientFallback implements UsuarioClient {
    @Override 
    public UsuarioResponseDTO getCurrentUsuario() {
        throw new ServiceUnavailableException("No se pudo obtener el usuario autenticado.");
    }
}

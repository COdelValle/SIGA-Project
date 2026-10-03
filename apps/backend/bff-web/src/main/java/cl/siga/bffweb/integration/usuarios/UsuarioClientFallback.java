package cl.siga.bffweb.integration.usuarios;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component 
public class UsuarioClientFallback implements UsuarioClient {
    @Override 
    public UsuarioResponseDTO getCurrentUsuario() {
        throw new ServiceUnavailableException("No se pudo obtener el usuario autenticado.");
    }

    @Override
    public PageResponseDTO<UsuarioResponseDTO> searchUsuarios(String email, Rol rol, StateUsuario state, int size) {
        throw new ServiceUnavailableException("No se pudieron obtener los usuarios.");
    }
}

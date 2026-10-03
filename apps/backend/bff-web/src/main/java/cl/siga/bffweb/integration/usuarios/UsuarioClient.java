package cl.siga.bffweb.integration.usuarios;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;

@FeignClient (
    name = "ms-usuarios-auth",
    url = "${services.usuarios.url}",
    fallbackFactory = UsuarioClientFallbackFactory.class
)
public interface UsuarioClient {
    @GetMapping ("/api/v1/usuarios/me")
    UsuarioResponseDTO getCurrentUsuario();

    @GetMapping ("/api/v1/usuarios/search")
    PageResponseDTO<UsuarioResponseDTO> searchUsuarios(
        @RequestParam (value = "email", required = false) String email,
        @RequestParam (value = "rol", required = false) Rol rol,
        @RequestParam (value = "state", required = false) StateUsuario state,
        @RequestParam ("size") int size);
}

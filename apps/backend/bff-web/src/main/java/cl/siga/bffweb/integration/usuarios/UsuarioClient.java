package cl.siga.bffweb.integration.usuarios;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import cl.siga.bffweb.config.FeignClientConfig;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;

@FeignClient (
    name = "ms-usuarios-auth",
    url = "${microservices.usuarios.url}",
    configuration = FeignClientConfig.class
)
public interface UsuarioClient {
    @GetMapping ("/api/v1/usuarios/me")
    UsuarioResponseDTO getCurrentUsuario();
}

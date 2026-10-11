package cl.siga.bffweb.integration.usuarios;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioCompuestoRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationCredentialResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStatusResponseDTO;
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

    @GetMapping ("/api/v1/usuarios/{id}")
    UsuarioResponseDTO getUsuarioById(@PathVariable ("id") String id);

    @GetMapping ("/api/v1/usuarios/search")
    PageResponseDTO<UsuarioResponseDTO> searchUsuarios(
        @RequestParam (value = "email", required = false) String email,
        @RequestParam (value = "rol", required = false) Rol rol,
        @RequestParam (value = "state", required = false) StateUsuario state,
        @RequestParam ("size") int size);

    @PostMapping ("/api/v1/usuarios/registraciones/async")
    UserRegistrationStatusResponseDTO iniciarRegistroCompuesto(
        @RequestBody RegistrarUsuarioCompuestoRequestDTO request);

    @GetMapping ("/api/v1/usuarios/registraciones/{processId}")
    UserRegistrationStatusResponseDTO getRegistroCompuesto(@PathVariable ("processId") String processId);

    @GetMapping ("/api/v1/usuarios/registraciones/{processId}/credencial")
    UserRegistrationCredentialResponseDTO getCredencialTemporal(@PathVariable ("processId") String processId);

    @PostMapping ("/api/v1/usuarios/{id}/reset-password")
    UserRegistrationCredentialResponseDTO resetPassword(@PathVariable ("id") String id);

    @DeleteMapping ("/api/v1/usuarios/{id}")
    void deleteUsuario(@PathVariable ("id") String id);
}

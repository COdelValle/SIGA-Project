package cl.siga.bffweb.integration.usuarios;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.bffweb.integration.FeignErrorTranslator;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioCompuestoRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationCredentialResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStatusResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fallback del cliente de usuarios que conserva el 404 ("usuario no registrado")
 * para que el frontend muestre /sin-acceso, traduce los errores de negocio de
 * los endpoints de registro (400/403/404/409) y usa 503 solo ante fallos reales.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioClientFallbackFactory implements FallbackFactory<UsuarioClient> {

    private final FeignErrorTranslator errorTranslator;

    @Override
    public UsuarioClient create(Throwable cause) {
        return new UsuarioClient() {
            @Override
            public UsuarioResponseDTO getCurrentUsuario() {
                if (cause instanceof FeignException.NotFound) {
                    log.warn("El usuario autenticado no esta registrado en SIGA (404 de ms-usuarios-auth).");
                    throw new ResourceNotFoundException("El usuario autenticado no esta registrado en SIGA.");
                }
                throw noDisponible(cause, "No se pudo obtener el usuario autenticado.");
            }

            @Override
            public UsuarioResponseDTO getUsuarioById(String id) {
                throw errorTranslator.traducir(cause, "No se pudo obtener el usuario " + id + ".");
            }

            @Override
            public PageResponseDTO<UsuarioResponseDTO> searchUsuarios(
                    String email, Rol rol, StateUsuario state, int size) {
                throw noDisponible(cause, "No se pudieron obtener los usuarios.");
            }

            @Override
            public UserRegistrationStatusResponseDTO iniciarRegistroCompuesto(
                    RegistrarUsuarioCompuestoRequestDTO request) {
                throw errorTranslator.traducir(cause, "No se pudo iniciar el registro del usuario.");
            }

            @Override
            public UserRegistrationStatusResponseDTO getRegistroCompuesto(String processId) {
                throw errorTranslator.traducir(cause, "No se pudo obtener el estado del registro.");
            }

            @Override
            public UserRegistrationCredentialResponseDTO getCredencialTemporal(String processId) {
                throw errorTranslator.traducir(cause, "No se pudo obtener la credencial temporal.");
            }

            @Override
            public UserRegistrationCredentialResponseDTO resetPassword(String id) {
                throw errorTranslator.traducir(cause, "No se pudo restablecer la contraseña.");
            }

            @Override
            public void deleteUsuario(String id) {
                throw errorTranslator.traducir(cause, "No se pudo eliminar el usuario.");
            }
        };
    }

    private ServiceUnavailableException noDisponible(Throwable cause, String mensaje) {
        int status = cause instanceof FeignException feign ? feign.status() : -1;
        log.error("Fallo al llamar a ms-usuarios-auth (status={}): {}", status, cause.getMessage());
        return new ServiceUnavailableException(mensaje + " (HTTP " + status + ")");
    }
}

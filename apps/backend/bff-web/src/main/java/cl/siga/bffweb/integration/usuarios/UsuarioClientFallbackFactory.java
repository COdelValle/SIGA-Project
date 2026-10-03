package cl.siga.bffweb.integration.usuarios;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

/**
 * Fallback del cliente de usuarios que conserva el 404 ("usuario no registrado")
 * para que el frontend muestre /sin-acceso, y usa 503 solo ante fallos reales.
 * Registra la causa para diagnostico.
 */
@Slf4j
@Component
public class UsuarioClientFallbackFactory implements FallbackFactory<UsuarioClient> {

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
            public PageResponseDTO<UsuarioResponseDTO> searchUsuarios(
                    String email, Rol rol, StateUsuario state, int size) {
                throw noDisponible(cause, "No se pudieron obtener los usuarios.");
            }
        };
    }

    private ServiceUnavailableException noDisponible(Throwable cause, String mensaje) {
        int status = cause instanceof FeignException feign ? feign.status() : -1;
        log.error("Fallo al llamar a ms-usuarios-auth (status={}): {}", status, cause.getMessage());
        return new ServiceUnavailableException(mensaje + " (HTTP " + status + ")");
    }
}

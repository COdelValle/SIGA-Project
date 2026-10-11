package cl.siga.coreshare.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

/**
 * Construye la ServiceUnavailableException de los fallbacks Feign incluyendo el
 * status real de la causa. Asi un error de configuracion (por ejemplo una URL
 * mal resuelta) queda visible en los logs y en el mensaje, en vez de un 503
 * opaco sin diagnostico.
 */
@Slf4j
public final class FeignFallbacks {

    private FeignFallbacks() {
    }

    public static ServiceUnavailableException noDisponible(Throwable cause, String mensaje) {
        int status = cause instanceof FeignException feign ? feign.status() : -1;
        log.error("Fallo en llamada Feign (status={}): {}", status, cause.getMessage());
        return new ServiceUnavailableException(mensaje + " (HTTP " + status + ")");
    }
}

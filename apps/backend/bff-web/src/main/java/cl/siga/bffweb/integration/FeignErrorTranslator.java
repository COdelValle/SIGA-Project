package cl.siga.bffweb.integration;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ConflictException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Traduce los errores de los microservicios a excepciones del BFF para no
 * enmascarar 400/403/404/409 como 503. Solo los fallos reales (conexion, 5xx,
 * 401) se convierten en ServiceUnavailableException. Si el microservicio
 * devuelve un cuerpo de error con `message`, se conserva.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeignErrorTranslator {
    private final ObjectMapper objectMapper;

    public RuntimeException traducir(Throwable cause, String contexto) {
        int status = cause instanceof FeignException feign ? feign.status() : -1;
        String mensaje = mensajeDe(cause);
        if (status == 404) {
            return new ResourceNotFoundException(mensaje != null ? mensaje : contexto);
        }
        if (status == 400) {
            return new BusinessException(mensaje != null ? mensaje : contexto);
        }
        if (status == 409) {
            return new ConflictException(mensaje != null ? mensaje : contexto);
        }
        if (status == 403) {
            return new AccessDeniedException(
                mensaje != null ? mensaje : "No tienes permisos para realizar esta acción.");
        }
        log.error("Fallo al llamar a un microservicio (status={}): {}", status, cause.getMessage());
        return new ServiceUnavailableException(contexto + " (HTTP " + status + ")");
    }

    private String mensajeDe(Throwable cause) {
        if (!(cause instanceof FeignException feign)) {
            return null;
        }
        try {
            JsonNode cuerpo = objectMapper.readTree(feign.contentUTF8());
            JsonNode mensaje = cuerpo.path("message");
            if (!mensaje.isMissingNode() && !mensaje.isNull() && !mensaje.asText().isBlank()) {
                return mensaje.asText();
            }
        } catch (Exception ignorado) {
            // El cuerpo no es JSON o no trae el campo message.
        }
        return null;
    }
}

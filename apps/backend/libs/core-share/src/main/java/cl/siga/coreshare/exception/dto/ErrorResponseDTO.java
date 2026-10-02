package cl.siga.coreshare.exception.dto;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public record ErrorResponseDTO(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    Map<String, String> validationErrors,
    String path,
    String correlationId
) {

    /** Construye la respuesta con la ruta y el correlation id de la peticion actual. */
    public static ErrorResponseDTO of(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> validationErrors) {
        return new ErrorResponseDTO(
            LocalDateTime.now(),
            status.value(),
            error,
            message,
            validationErrors,
            currentPath(),
            MDC.get("correlationId")
        );
    }

    private static String currentPath() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRequestURI();
        }
        return null;
    }
}

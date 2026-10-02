package cl.siga.coreshare.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import cl.siga.coreshare.exception.dto.ErrorResponseDTO;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@RestControllerAdvice 
public class GlobalExceptionHandler {

    // 1. Validaciones de DTOs (@Valid)
    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        log.warn("Datos de entrada invalidos: {}", errors);
        return ResponseEntity.badRequest().body(ErrorResponseDTO.of(
            HttpStatus.BAD_REQUEST, "Validation Error", "Datos de entrada inválidos", errors));
    }

    // 1b. Validaciones de parámetros (@Validated en servicios/controladores)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
            errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        log.warn("Parametros invalidos: {}", errors);
        return ResponseEntity.badRequest().body(ErrorResponseDTO.of(
            HttpStatus.BAD_REQUEST, "Validation Error", "Parámetros inválidos", errors));
    }

    // 1c. Cuerpo o formato de dato ilegible (JSON inválido, enum/fecha inválida)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo o formato de datos invalido: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ErrorResponseDTO.of(
            HttpStatus.BAD_REQUEST, "Bad Request", "Cuerpo o formato de datos inválido", null));
    }

    // 1d. Parametro de URL con tipo invalido (p. ej. enum o fecha mal formada)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "El parametro '" + ex.getName() + "' tiene un formato invalido.";
        log.warn("{} Valor recibido: {}", detail, ex.getValue());
        return ResponseEntity.badRequest().body(ErrorResponseDTO.of(
            HttpStatus.BAD_REQUEST, "Bad Request", detail, null));
    }

    // 1e. Parametro obligatorio ausente
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParameter(MissingServletRequestParameterException ex) {
        String detail = "Falta el parametro obligatorio '" + ex.getParameterName() + "'.";
        log.warn(detail);
        return ResponseEntity.badRequest().body(ErrorResponseDTO.of(
            HttpStatus.BAD_REQUEST, "Bad Request", detail, null));
    }

    // 1f. Sort con un campo que no existe en la entidad
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponseDTO> handlePropertyReference(PropertyReferenceException ex) {
        String detail = "El parametro 'sort' contiene un campo invalido: '" + ex.getPropertyName() + "'.";
        log.warn(detail);
        return ResponseEntity.badRequest().body(ErrorResponseDTO.of(
            HttpStatus.BAD_REQUEST, "Bad Request", detail, null));
    }

    // 2. Recursos no encontrados (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex) {
        log.debug("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponseDTO.of(
            HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), null));
    }

    // 2b. Rutas o recursos estáticos inexistentes (404 real, sin enmascarar como 500)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResource(NoResourceFoundException ex) {
        log.debug("Ruta no encontrada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponseDTO.of(
            HttpStatus.NOT_FOUND, "Not Found", "Recurso no encontrado", null));
    }

    // 2c. Metodo HTTP no soportado por el endpoint (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Metodo HTTP no soportado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ErrorResponseDTO.of(
            HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", "Método HTTP no soportado para este recurso", null));
    }

    // 3. Errores de Negocio o Solicitud Incorrecta (400)
    @ExceptionHandler({BadRequestException.class, BusinessException.class})
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(RuntimeException ex) {
        log.warn("Solicitud invalida: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ErrorResponseDTO.of(
            HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), null));
    }

    // 3b. Acceso denegado (403) lanzado desde la capa de servicio
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponseDTO.of(
            HttpStatus.FORBIDDEN, "Forbidden",
            ex.getMessage() != null ? ex.getMessage() : "No tienes permisos para realizar esta acción", null));
    }

    // 4. Conflictos de integridad de datos (409)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Conflicto de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponseDTO.of(
            HttpStatus.CONFLICT, "Conflict", "Conflicto de integridad de datos", null));
    }

    // 5. Servicio no disponible (503)
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Servicio no disponible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponseDTO.of(
            HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.getMessage(), null));
    }

    // 6. Errores no controlados (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
        log.error("Error inesperado no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponseDTO.of(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Error", "Ha ocurrido un error inesperado", null));
    }
}

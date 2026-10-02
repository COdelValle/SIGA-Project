package cl.siga.coreshare.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
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
        
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Validation Error", "Datos de entrada inválidos", errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    // 1b. Validaciones de parámetros (@Validated en servicios/controladores)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
            errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        log.warn("Parametros invalidos: {}", errors);

        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Validation Error", "Parámetros inválidos", errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    // 1c. Cuerpo o formato de dato ilegible (JSON inválido, enum/fecha inválida)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo o formato de datos invalido: {}", ex.getMessage());
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", "Cuerpo o formato de datos inválido", null
        );
        return ResponseEntity.badRequest().body(response);
    }

    // 1d. Parametro de URL con tipo invalido (p. ej. enum o fecha mal formada)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "El parametro '" + ex.getName() + "' tiene un formato invalido.";
        log.warn("{} Valor recibido: {}", detail, ex.getValue());
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", detail, null
        );
        return ResponseEntity.badRequest().body(response);
    }

    // 1e. Parametro obligatorio ausente
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParameter(MissingServletRequestParameterException ex) {
        String detail = "Falta el parametro obligatorio '" + ex.getParameterName() + "'.";
        log.warn(detail);
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", detail, null
        );
        return ResponseEntity.badRequest().body(response);
    }

    // 2. Recursos no encontrados (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex) {
        log.debug("Recurso no encontrado: {}", ex.getMessage());
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 2b. Rutas o recursos estáticos inexistentes (404 real, sin enmascarar como 500)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResource(NoResourceFoundException ex) {
        log.debug("Ruta no encontrada: {}", ex.getMessage());
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "Not Found", "Recurso no encontrado", null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 2c. Metodo HTTP no soportado por el endpoint (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Metodo HTTP no soportado: {}", ex.getMessage());
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.METHOD_NOT_ALLOWED.value(), "Method Not Allowed",
            "Método HTTP no soportado para este recurso", null
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    // 3. Errores de Negocio o Solicitud Incorrecta (400)
    @ExceptionHandler({BadRequestException.class, BusinessException.class})
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(RuntimeException ex) {
        log.warn("Solicitud invalida: {}", ex.getMessage());
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), null
        );
        return ResponseEntity.badRequest().body(response);
    }

    // 3b. Acceso denegado (403) lanzado desde la capa de servicio
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.FORBIDDEN.value(), "Forbidden",
            ex.getMessage() != null ? ex.getMessage() : "No tienes permisos para realizar esta acción", null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 4. Conflictos de integridad de datos (409)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Conflicto de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.CONFLICT.value(), "Conflict", "Conflicto de integridad de datos", null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 5. Servicio no disponible (503)
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Servicio no disponible: {}", ex.getMessage());
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable", ex.getMessage(), null
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // 6. Errores no controlados (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
        log.error("Error inesperado no controlado", ex);
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Error", "Ha ocurrido un error inesperado", null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

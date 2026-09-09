package cl.siga.coreshare.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cl.siga.coreshare.exception.dto.ErrorResponseDTO;

@RestControllerAdvice 
public class GlobalExceptionHandler {

    // 1. Validaciones de DTOs (@Valid)
    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Validation Error", "Datos de entrada inválidos", errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    // 2. Recursos no encontrados (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 3. Errores de Negocio o Solicitud Incorrecta (400)
    @ExceptionHandler({BadRequestException.class, BusinessException.class})
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(RuntimeException ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), null
        );
        return ResponseEntity.badRequest().body(response);
    }

    // 4. Servicio no disponible (503)
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleServiceUnavailable(ServiceUnavailableException ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable", ex.getMessage(), null
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // 5. Errores no controlados (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Error", "Ha ocurrido un error inesperado", null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
package cl.siga.coreshare.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cl.siga.coreshare.exception.dto.ErrorResponseDTO;
import jakarta.validation.ConstraintViolationException;

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

    // 1b. Validaciones de parámetros (@Validated en servicios/controladores)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
            errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Validation Error", "Parámetros inválidos", errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    // 1c. Cuerpo o formato de dato ilegible (JSON inválido, enum/fecha inválida)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", "Cuerpo o formato de datos inválido", null
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

    // 4. Conflictos de integridad de datos (409)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.CONFLICT.value(), "Conflict", "Conflicto de integridad de datos", null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 5. Servicio no disponible (503)
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleServiceUnavailable(ServiceUnavailableException ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable", ex.getMessage(), null
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // 6. Errores no controlados (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Error", "Ha ocurrido un error inesperado", null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

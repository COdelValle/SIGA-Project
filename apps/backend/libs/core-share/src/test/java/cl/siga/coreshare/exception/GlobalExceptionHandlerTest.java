package cl.siga.coreshare.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void recursoNoEncontradoDevuelve404() {
        var response = handler.handleNotFound(new ResourceNotFoundException("no existe"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("no existe", response.getBody().message());
    }

    @Test
    void errorDeNegocioDevuelve400() {
        var response = handler.handleBadRequest(new BusinessException("regla de negocio"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("regla de negocio", response.getBody().message());
    }

    @Test
    void accesoDenegadoDevuelve403() {
        var response = handler.handleAccessDenied(new AccessDeniedException("sin permiso"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("sin permiso", response.getBody().message());
    }

    @Test
    void conflictoDeIntegridadDevuelve409() {
        var response = handler.handleDataIntegrity(new DataIntegrityViolationException("duplicado"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void cuerpoIlegibleDevuelve400ConDetalle() {
        var response = handler.handleNotReadable(
            new HttpMessageNotReadableException("JSON parse error: campo invalido"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().message().contains("campo invalido"));
    }

    @Test
    void conflictoDeEstadoDevuelve409() {
        var response = handler.handleConflict(new ConflictException("ya existe"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("ya existe", response.getBody().message());
    }

    @Test
    void servicioNoDisponibleDevuelve503() {
        var response = handler.handleServiceUnavailable(new ServiceUnavailableException("ms caido"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void metodoNoSoportadoDevuelve405() {
        var response = handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("PATCH"));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    void parametroFaltanteDevuelve400ConDetalle() {
        var response = handler.handleMissingParameter(new MissingServletRequestParameterException("id", "Long"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().message().contains("id"));
    }

    @Test
    void errorInesperadoDevuelve500() {
        var response = handler.handleGeneric(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}

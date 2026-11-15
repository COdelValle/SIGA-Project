package cl.siga.coreshare.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

class GlobalExceptionHandlerExtraTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validacionDeDtoDevuelve400ConErroresPorCampo() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "email", "El correo es obligatorio"),
                new FieldError("request", "rol", "El rol es obligatorio")));

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().validationErrors())
                .containsEntry("email", "El correo es obligatorio")
                .containsEntry("rol", "El rol es obligatorio");
    }

    @Test
    void validacionDeParametrosDevuelve400ConErrores() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("searchUsuarios.email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("debe ser un correo");
        ConstraintViolationException ex =
                new ConstraintViolationException(Set.of(violation));

        var response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().validationErrors())
                .containsEntry("searchUsuarios.email", "debe ser un correo");
    }

    @Test
    void tipoDeParametroInvalidoDevuelve400ConElNombre() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("rol");
        when(ex.getValue()).thenReturn("PROFESOR");

        var response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("rol");
    }

    @Test
    void sortConCampoInvalidoDevuelve400ConElCampo() {
        PropertyReferenceException ex = mock(PropertyReferenceException.class);
        when(ex.getPropertyName()).thenReturn("correo");

        var response = handler.handlePropertyReference(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("correo");
    }

    @Test
    void rutaInexistenteDevuelve404Generico() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getMessage()).thenReturn("No static resource");

        var response = handler.handleNoResource(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Recurso no encontrado");
    }

    @Test
    void accesoDenegadoSinMensajeUsaTextoPorDefecto() {
        var response = handler.handleAccessDenied(
                new org.springframework.security.access.AccessDeniedException(null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message())
                .isEqualTo("No tienes permisos para realizar esta acción");
    }
}

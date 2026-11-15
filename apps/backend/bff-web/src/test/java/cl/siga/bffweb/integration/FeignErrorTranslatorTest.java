package cl.siga.bffweb.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ConflictException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;

class FeignErrorTranslatorTest {

    private final FeignErrorTranslator translator = new FeignErrorTranslator(new ObjectMapper());

    private FeignException feignConCuerpo(int status, String cuerpo) {
        FeignException causa = mock(FeignException.class);
        when(causa.status()).thenReturn(status);
        when(causa.contentUTF8()).thenReturn(cuerpo);
        when(causa.getMessage()).thenReturn("fallo remoto");
        return causa;
    }

    @Test
    void traduce404ConservandoElMensajeDelMicroservicio() {
        RuntimeException error = translator.traducir(
            feignConCuerpo(404, "{\"message\":\"La nota 9 no existe.\"}"), "No se pudo obtener la nota.");

        assertThat(error)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("La nota 9 no existe.");
    }

    @Test
    void traduce404ConCuerpoInvalidoUsandoElContexto() {
        RuntimeException error = translator.traducir(
            feignConCuerpo(404, "texto plano"), "No se pudo obtener la nota.");

        assertThat(error)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("No se pudo obtener la nota.");
    }

    @Test
    void traduce404SinCampoMessageUsandoElContexto() {
        RuntimeException error = translator.traducir(
            feignConCuerpo(404, "{\"message\":\"  \"}"), "No se pudo obtener la nota.");

        assertThat(error)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("No se pudo obtener la nota.");
    }

    @Test
    void traduce400ComoBusinessException() {
        RuntimeException error = translator.traducir(
            feignConCuerpo(400, "{\"message\":\"Ponderacion invalida.\"}"), "No se pudo registrar.");

        assertThat(error)
            .isInstanceOf(BusinessException.class)
            .hasMessage("Ponderacion invalida.");
    }

    @Test
    void traduce409ComoConflictException() {
        RuntimeException error = translator.traducir(
            feignConCuerpo(409, "{\"message\":\"La nota ya existe.\"}"), "No se pudo registrar.");

        assertThat(error)
            .isInstanceOf(ConflictException.class)
            .hasMessage("La nota ya existe.");
    }

    @Test
    void traduce403ComoAccessDeniedConservandoElMensaje() {
        RuntimeException error = translator.traducir(
            feignConCuerpo(403, "{\"message\":\"Sin permiso sobre la dictacion.\"}"), "No se pudo registrar.");

        assertThat(error)
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Sin permiso sobre la dictacion.");
    }

    @Test
    void traduce403SinMensajeConElTextoPorDefecto() {
        RuntimeException error = translator.traducir(
            feignConCuerpo(403, ""), "No se pudo registrar.");

        assertThat(error)
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageStartingWith("No tienes permisos para realizar esta acci");
    }

    @Test
    void traduce5xxComoServicioNoDisponibleIncluyendoElStatus() {
        RuntimeException error = translator.traducir(
            feignConCuerpo(502, "{\"message\":\"gateway caido\"}"), "No se pudo obtener la nota.");

        assertThat(error)
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessage("No se pudo obtener la nota. (HTTP 502)");
    }

    @Test
    void traduceFallosSinStatusComoServicioNoDisponibleConMenosUno() {
        RuntimeException causa = new RuntimeException("conexion rechazada");

        RuntimeException error = translator.traducir(causa, "No se pudo obtener la nota.");

        assertThat(error)
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessage("No se pudo obtener la nota. (HTTP -1)");
    }

    @Test
    void traduceCuerpoNuloComoContexto() {
        FeignException causa = mock(FeignException.class);
        doReturn(404).when(causa).status();
        when(causa.contentUTF8()).thenReturn(null);

        assertThatThrownBy(() -> {
            throw translator.traducir(causa, "No se pudo obtener la nota.");
        })
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("No se pudo obtener la nota.");
    }
}

package cl.siga.msusuariosauth.integration.estudiantes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.Request;

@ExtendWith(MockitoExtension.class)
class EstudianteDirectoryTest {

    @Mock
    private EstudianteClient estudianteClient;

    private EstudianteDirectory directory;

    @BeforeEach
    void setUp() {
        directory = new EstudianteDirectory(estudianteClient);
    }

    private Request request() {
        return Request.create(Request.HttpMethod.GET, "http://localhost/api/v1/estudiantes/exists/1",
                Map.of(), null, StandardCharsets.UTF_8);
    }

    @Test
    void listaNulaNoValidaNada() {
        assertDoesNotThrow(() -> directory.validarVinculos(null));
        verifyNoInteractions(estudianteClient);
    }

    @Test
    void vinculoNuloEsRechazado() {
        List<ParentescoEstudianteDTO> vinculos = java.util.Collections.singletonList(null);

        assertThrows(BusinessException.class, () -> directory.validarVinculos(vinculos));
    }

    @Test
    void vinculoSinEstudianteEsRechazado() {
        List<ParentescoEstudianteDTO> vinculos =
                List.of(new ParentescoEstudianteDTO(null, Parentesco.MADRE_PADRE));

        assertThrows(BusinessException.class, () -> directory.validarVinculos(vinculos));
    }

    @Test
    void estudianteExistenteEsAprobado() {
        when(estudianteClient.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> directory.validarVinculos(
                List.of(new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE))));
        verify(estudianteClient).existsById(1L);
    }

    @Test
    void estudianteInexistenteEsRechazado() {
        when(estudianteClient.existsById(1L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> directory.validarVinculos(
                List.of(new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE))));
    }

    @Test
    void respuestaNulaSeConsideraInexistente() {
        when(estudianteClient.existsById(1L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> directory.validarVinculos(
                List.of(new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE))));
    }

    @Test
    void notFoundDeFeignSeTraduceABusinessException() {
        when(estudianteClient.existsById(1L)).thenThrow(
                new FeignException.NotFound("no encontrado", request(), new byte[0], Map.of()));

        assertThrows(BusinessException.class, () -> directory.validarVinculos(
                List.of(new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE))));
    }

    @Test
    void errorDeFeignSeTraduceAServiceUnavailable() {
        FeignException error = mock(FeignException.class);
        when(error.status()).thenReturn(500);
        when(estudianteClient.existsById(1L)).thenThrow(error);

        assertThrows(ServiceUnavailableException.class, () -> directory.validarVinculos(
                List.of(new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE))));
    }
}

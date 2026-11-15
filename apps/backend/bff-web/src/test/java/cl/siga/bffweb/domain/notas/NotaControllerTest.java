package cl.siga.bffweb.domain.notas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;

class NotaControllerTest {

    private final NotaBffService service = mock(NotaBffService.class);
    private final NotaController controller = new NotaController(service);

    @Test
    void crearNotaRespondeCreated() {
        RegistrarNotaRequestDTO request = mock(RegistrarNotaRequestDTO.class);
        NotaResponseDTO creada = mock(NotaResponseDTO.class);
        when(service.crearNota(request)).thenReturn(creada);

        var respuesta = controller.crearNota(request);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(201);
        assertThat(respuesta.getBody()).isSameAs(creada);
        verify(service).crearNota(request);
    }

    @Test
    void editarNotaDelegaEnElServicio() {
        ActualizarNotaRequestDTO request = mock(ActualizarNotaRequestDTO.class);
        NotaResponseDTO editada = mock(NotaResponseDTO.class);
        when(service.editarNota(7L, request)).thenReturn(editada);

        var respuesta = controller.editarNota(7L, request);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isSameAs(editada);
        verify(service).editarNota(7L, request);
    }

    @Test
    void eliminarNotaResponde204() {
        var respuesta = controller.eliminarNota(7L);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(204);
        verify(service).eliminarNota(7L);
    }
}

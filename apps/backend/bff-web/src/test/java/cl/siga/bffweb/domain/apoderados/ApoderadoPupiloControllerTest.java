package cl.siga.bffweb.domain.apoderados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;

class ApoderadoPupiloControllerTest {

    private final ApoderadoPupiloService service = mock(ApoderadoPupiloService.class);
    private final ApoderadoPupiloController controller = new ApoderadoPupiloController(service);

    @Test
    void pupilosDelApoderadoDeleganEnElServicio() {
        when(service.getPupilos()).thenReturn(List.of());

        var respuesta = controller.getPupilos();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isEmpty();
        verify(service).getPupilos();
    }

    @Test
    void actualizarPupiloDelegaEnElServicio() {
        ActualizarEstudianteRequestDTO request = mock(ActualizarEstudianteRequestDTO.class);
        EstudianteResponseDTO actualizado = mock(EstudianteResponseDTO.class);
        when(service.actualizarPupilo(2L, request)).thenReturn(actualizado);

        var respuesta = controller.actualizarPupilo(2L, request);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isSameAs(actualizado);
        verify(service).actualizarPupilo(2L, request);
    }
}

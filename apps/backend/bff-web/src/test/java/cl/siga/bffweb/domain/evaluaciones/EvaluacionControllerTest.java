package cl.siga.bffweb.domain.evaluaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;

class EvaluacionControllerTest {

    private final EvaluacionBffService service = mock(EvaluacionBffService.class);
    private final EvaluacionController controller = new EvaluacionController(service);

    @Test
    void crearEvaluacionRespondeCreated() {
        RegistrarEvaluacionRequestDTO request = mock(RegistrarEvaluacionRequestDTO.class);
        EvaluacionResponseDTO creada = mock(EvaluacionResponseDTO.class);
        when(service.crearEvaluacion(request)).thenReturn(creada);

        var respuesta = controller.crearEvaluacion(request);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(201);
        assertThat(respuesta.getBody()).isSameAs(creada);
        verify(service).crearEvaluacion(request);
    }

    @Test
    void editarEvaluacionDelegaEnElServicio() {
        ActualizarEvaluacionRequestDTO request = mock(ActualizarEvaluacionRequestDTO.class);
        EvaluacionResponseDTO editada = mock(EvaluacionResponseDTO.class);
        when(service.editarEvaluacion(9L, request)).thenReturn(editada);

        var respuesta = controller.editarEvaluacion(9L, request);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isSameAs(editada);
        verify(service).editarEvaluacion(9L, request);
    }

    @Test
    void eliminarEvaluacionResponde204() {
        var respuesta = controller.eliminarEvaluacion(9L);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(204);
        verify(service).eliminarEvaluacion(9L);
    }
}

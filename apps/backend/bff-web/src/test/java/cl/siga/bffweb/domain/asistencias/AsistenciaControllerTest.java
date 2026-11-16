package cl.siga.bffweb.domain.asistencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;

class AsistenciaControllerTest {

    private final AsistenciaBffService service = mock(AsistenciaBffService.class);
    private final AsistenciaController controller = new AsistenciaController(service);

    @Test
    void asistenciasDelEstudianteAutenticado() {
        AsistenciaResponseDTO asistencia = mock(AsistenciaResponseDTO.class);
        when(service.getAsistenciasMe()).thenReturn(List.of(asistencia));

        var respuesta = controller.getAsistenciasMe();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).containsExactly(asistencia);
        verify(service).getAsistenciasMe();
    }

    @Test
    void asistenciasPorEstudianteDeleganEnElServicio() {
        when(service.getAsistenciasEstudiante(7L)).thenReturn(List.of());

        var respuesta = controller.getAsistenciasEstudiante(7L);

        assertThat(respuesta.getBody()).isEmpty();
        verify(service).getAsistenciasEstudiante(7L);
    }

    @Test
    void asistenciasPorAsignaturaYFechaDeleganEnElServicio() {
        LocalDate fecha = LocalDate.of(2026, 10, 2);
        when(service.getAsistenciasAsignatura(5L, fecha)).thenReturn(List.of());

        var respuesta = controller.getAsistenciasAsignatura(5L, fecha);

        assertThat(respuesta.getBody()).isEmpty();
        verify(service).getAsistenciasAsignatura(5L, fecha);
    }

    @Test
    void registrarAsistenciaRespondeCreated() {
        RegistrarAsistenciaRequestDTO request = mock(RegistrarAsistenciaRequestDTO.class);
        AsistenciaResponseDTO creada = mock(AsistenciaResponseDTO.class);
        when(service.registrarAsistencia(request)).thenReturn(creada);

        var respuesta = controller.registrarAsistencia(request);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(201);
        assertThat(respuesta.getBody()).isSameAs(creada);
        verify(service).registrarAsistencia(request);
    }

    @Test
    void actualizarAsistenciaDelegaEnElServicio() {
        ActualizarAsistenciaRequestDTO request = mock(ActualizarAsistenciaRequestDTO.class);
        AsistenciaResponseDTO actualizada = mock(AsistenciaResponseDTO.class);
        when(service.actualizarAsistencia(1L, request)).thenReturn(actualizada);

        var respuesta = controller.actualizarAsistencia(1L, request);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isSameAs(actualizada);
        verify(service).actualizarAsistencia(1L, request);
    }
}

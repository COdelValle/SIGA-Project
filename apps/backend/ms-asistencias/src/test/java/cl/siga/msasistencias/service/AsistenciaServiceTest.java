package cl.siga.msasistencias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasistencias.client.AsignaturaClient;
import cl.siga.msasistencias.client.EstudianteClient;
import cl.siga.msasistencias.model.entity.Asistencia;
import cl.siga.msasistencias.model.mapper.AsistenciaMapperImpl;
import cl.siga.msasistencias.repository.AsistenciaRepository;

class AsistenciaServiceTest {

    private final AsistenciaRepository repository = mock(AsistenciaRepository.class);
    private final EstudianteClient estudianteClient = mock(EstudianteClient.class);
    private final AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);

    private final AsistenciaService service = new AsistenciaService(
        repository, new AsistenciaMapperImpl(), estudianteClient, asignaturaClient);

    private final LocalDate fecha = LocalDate.of(2026, 10, 1);

    @BeforeEach
    void existenciasOk() {
        when(estudianteClient.existsById(1L)).thenReturn(true);
        when(asignaturaClient.existsById(5L)).thenReturn(true);
        when(repository.save(any(Asistencia.class))).thenAnswer(invocacion -> {
            Asistencia asistencia = invocacion.getArgument(0);
            if (asistencia.getId() == null) {
                asistencia.setId(100L);
            }
            return asistencia;
        });
    }

    @Test
    void presenteQuedaConJustificacionNoAplica() {
        when(repository.existsByIdEstudianteAndIdAsignaturaAndFecha(1L, 5L, fecha)).thenReturn(false);

        AsistenciaResponseDTO respuesta = service.saveAsistencia(
            new RegistrarAsistenciaRequestDTO(1L, 5L, fecha, State.PRESENTE, null));

        assertThat(respuesta.estado()).isEqualTo(State.PRESENTE);
        assertThat(respuesta.justificacion()).isEqualTo(Justificacion.NO_APLICA);
        assertThat(respuesta.idEstudiante()).isEqualTo(1L);
        assertThat(respuesta.idAsignatura()).isEqualTo(5L);
    }

    @Test
    void ausenteQuedaConJustificacionPendiente() {
        when(repository.existsByIdEstudianteAndIdAsignaturaAndFecha(1L, 5L, fecha)).thenReturn(false);

        AsistenciaResponseDTO respuesta = service.saveAsistencia(
            new RegistrarAsistenciaRequestDTO(1L, 5L, fecha, State.AUSENTE, "sin aviso"));

        assertThat(respuesta.estado()).isEqualTo(State.AUSENTE);
        assertThat(respuesta.justificacion()).isEqualTo(Justificacion.PENDIENTE);
    }

    @Test
    void rechazaDuplicadoDeEstudianteAsignaturaFecha() {
        when(repository.existsByIdEstudianteAndIdAsignaturaAndFecha(1L, 5L, fecha)).thenReturn(true);

        assertThatThrownBy(() -> service.saveAsistencia(
            new RegistrarAsistenciaRequestDTO(1L, 5L, fecha, State.PRESENTE, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Ya existe asistencia");
    }

    @Test
    void rechazaEstudianteInexistente() {
        when(estudianteClient.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.saveAsistencia(
            new RegistrarAsistenciaRequestDTO(99L, 5L, fecha, State.PRESENTE, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("estudiante no existe");
    }

    @Test
    void actualizaJustificacionDeUnaAusencia() {
        Asistencia existente = Asistencia.builder()
            .id(7L).idEstudiante(1L).idAsignatura(5L).fecha(fecha)
            .estado(State.AUSENTE).justificacion(Justificacion.PENDIENTE).active(true).build();
        when(repository.findByIdAndActiveTrue(7L)).thenReturn(java.util.Optional.of(existente));

        AsistenciaResponseDTO respuesta = service.updateAsistencia(7L,
            new ActualizarAsistenciaRequestDTO(Justificacion.SI, "certificado medico", null));

        assertThat(respuesta.justificacion()).isEqualTo(Justificacion.SI);
        assertThat(respuesta.observacion()).isEqualTo("certificado medico");
    }

    @Test
    void corrigeEstadoAAusenteYNormalizaJustificacion() {
        Asistencia existente = Asistencia.builder()
            .id(7L).idEstudiante(1L).idAsignatura(5L).fecha(fecha)
            .estado(State.PRESENTE).justificacion(Justificacion.NO_APLICA).active(true).build();
        when(repository.findByIdAndActiveTrue(7L)).thenReturn(java.util.Optional.of(existente));

        AsistenciaResponseDTO respuesta = service.updateAsistencia(7L,
            new ActualizarAsistenciaRequestDTO(Justificacion.PENDIENTE, null, State.AUSENTE));

        assertThat(respuesta.estado()).isEqualTo(State.AUSENTE);
        assertThat(respuesta.justificacion()).isEqualTo(Justificacion.PENDIENTE);
    }

    @Test
    void corrigeEstadoAPresenteFuerzaJustificacionNoAplica() {
        Asistencia existente = Asistencia.builder()
            .id(7L).idEstudiante(1L).idAsignatura(5L).fecha(fecha)
            .estado(State.AUSENTE).justificacion(Justificacion.SI).active(true).build();
        when(repository.findByIdAndActiveTrue(7L)).thenReturn(java.util.Optional.of(existente));

        AsistenciaResponseDTO respuesta = service.updateAsistencia(7L,
            new ActualizarAsistenciaRequestDTO(Justificacion.NO_APLICA, null, State.PRESENTE));

        assertThat(respuesta.estado()).isEqualTo(State.PRESENTE);
        assertThat(respuesta.justificacion()).isEqualTo(Justificacion.NO_APLICA);
    }

    @Test
    void deleteHaceSoftDelete() {
        Asistencia existente = Asistencia.builder()
            .id(7L).idEstudiante(1L).idAsignatura(5L).fecha(fecha)
            .estado(State.PRESENTE).justificacion(Justificacion.NO_APLICA).active(true).build();
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(existente));

        service.deleteAsistencia(7L);

        assertThat(existente.getActive()).isFalse();
    }

    @Test
    void getInexistenteLanzaNotFound() {
        when(repository.findByIdAndActiveTrue(404L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getAsistenciaById(404L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}

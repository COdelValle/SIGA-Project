package cl.siga.msnotas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ConflictException;
import cl.siga.msnotas.client.EstudianteClient;
import cl.siga.msnotas.client.EvaluacionClient;
import cl.siga.msnotas.mensajeria.PublicadorNota;
import cl.siga.msnotas.model.entity.Nota;
import cl.siga.msnotas.model.mapper.NotaMapperImpl;
import cl.siga.msnotas.repository.NotaRepository;

class NotaServiceTest {

    private final NotaRepository repository = mock(NotaRepository.class);
    private final EstudianteClient estudianteClient = mock(EstudianteClient.class);
    private final EvaluacionClient evaluacionClient = mock(EvaluacionClient.class);
    private final PublicadorNota publicador = mock(PublicadorNota.class);

    private final NotaService service = new NotaService(
        repository, new NotaMapperImpl(), estudianteClient, evaluacionClient, publicador);

    @BeforeEach
    void existenciasOk() {
        when(estudianteClient.existsById(1L)).thenReturn(true);
        when(evaluacionClient.existsById(9L)).thenReturn(true);
        when(repository.save(any(Nota.class))).thenAnswer(invocacion -> {
            Nota nota = invocacion.getArgument(0);
            if (nota.getId() == null) {
                nota.setId(100L);
            }
            return nota;
        });
    }

    @Test
    void guardaNotaNueva() {
        when(repository.findByIdEstudianteAndIdEvaluacion(1L, 9L)).thenReturn(Optional.empty());

        NotaResponseDTO respuesta = service.saveNota(new RegistrarNotaRequestDTO(1L, 9L, 6.5));

        assertThat(respuesta.id()).isEqualTo(100L);
        assertThat(respuesta.idEstudiante()).isEqualTo(1L);
        assertThat(respuesta.idEvaluacion()).isEqualTo(9L);
        assertThat(respuesta.score()).isEqualTo(6.5);
    }

    @Test
    void rechazaDuplicadoActivo() {
        Nota existente = Nota.builder()
            .id(7L).idEstudiante(1L).idEvaluacion(9L).score(5.5).active(true).build();
        when(repository.findByIdEstudianteAndIdEvaluacion(1L, 9L))
            .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.saveNota(new RegistrarNotaRequestDTO(1L, 9L, 6.0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Ya existe una nota");
    }

    @Test
    void reactivaNotaInactivaEnLugarDeInsertar() {
        Nota inactiva = Nota.builder()
            .id(7L).idEstudiante(1L).idEvaluacion(9L).score(4.0).active(false).build();
        when(repository.findByIdEstudianteAndIdEvaluacion(1L, 9L))
            .thenReturn(Optional.of(inactiva));

        NotaResponseDTO respuesta = service.saveNota(new RegistrarNotaRequestDTO(1L, 9L, 6.2));

        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(respuesta.score()).isEqualTo(6.2);
        assertThat(inactiva.isActive()).isTrue();
    }

    @Test
    void rechazaEstudianteInexistente() {
        when(estudianteClient.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.saveNota(new RegistrarNotaRequestDTO(99L, 9L, 5.0)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("estudiante");
    }

    @Test
    void rechazaEvaluacionInexistente() {
        when(evaluacionClient.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.saveNota(new RegistrarNotaRequestDTO(1L, 404L, 5.0)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("evaluación");
    }

    @Test
    void guardarNotaNuevaPublicaCreada() {
        when(repository.findByIdEstudianteAndIdEvaluacion(1L, 9L)).thenReturn(Optional.empty());

        service.saveNota(new RegistrarNotaRequestDTO(1L, 9L, 6.5));

        org.mockito.Mockito.verify(publicador).publicarCreada(any(NotaResponseDTO.class));
        org.mockito.Mockito.verify(publicador, org.mockito.Mockito.never())
            .publicarActualizada(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reactivarNotaPublicaActualizadaYNoCreada() {
        Nota inactiva = Nota.builder()
            .id(7L).idEstudiante(1L).idEvaluacion(9L).score(4.0).active(false).build();
        when(repository.findByIdEstudianteAndIdEvaluacion(1L, 9L))
            .thenReturn(Optional.of(inactiva));

        service.saveNota(new RegistrarNotaRequestDTO(1L, 9L, 6.2));

        org.mockito.Mockito.verify(publicador).publicarActualizada(any(NotaResponseDTO.class));
        org.mockito.Mockito.verify(publicador, org.mockito.Mockito.never())
            .publicarCreada(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void actualizarNotaPublicaEventoActualizada() {
        Nota existente = Nota.builder()
            .id(15L).idEstudiante(1L).idEvaluacion(9L).score(4.0).active(true).build();
        when(repository.findByIdAndActiveTrue(15L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        NotaResponseDTO actualizada = service.updateNota(15L, new ActualizarNotaRequestDTO(6.2));

        assertThat(actualizada.score()).isEqualTo(6.2);
        verify(publicador).publicarActualizada(actualizada);
    }

    @Test
    void borradoLogicoDeNotaNoPublicaEvento() {
        Nota existente = Nota.builder()
            .id(16L).idEstudiante(1L).idEvaluacion(9L).score(5.0).active(true).build();
        when(repository.findById(16L)).thenReturn(Optional.of(existente));

        service.deleteNota(16L);

        assertThat(existente.isActive()).isFalse();
        verify(publicador, never()).publicarCreada(any(NotaResponseDTO.class));
        verify(publicador, never()).publicarActualizada(any(NotaResponseDTO.class));
    }
}

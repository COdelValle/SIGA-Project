package cl.siga.msasignaturas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.asignatura.inscripcion.RegistrarInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msasignaturas.client.EstudianteClient;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaBasica;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaElectiva;
import cl.siga.msasignaturas.model.mapper.InscripcionMapper;
import cl.siga.msasignaturas.repository.InscripcionRepository;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;

class InscripcionServiceTest {

    private InscripcionRepository inscripcionRepository;
    private AsignaturaRepository asignaturaRepository;
    private InscripcionMapper inscripcionMapper;
    private InscripcionService service;

    @BeforeEach
    void setUp() {
        inscripcionRepository = mock(InscripcionRepository.class);
        asignaturaRepository = mock(AsignaturaRepository.class);
        inscripcionMapper = mock(InscripcionMapper.class);
        service = new InscripcionService(
                inscripcionRepository,
                asignaturaRepository,
                inscripcionMapper,
                mock(EstudianteClient.class));
    }

    private AsignaturaElectiva electiva(Long id, int cupo) {
        AsignaturaElectiva electiva = new AsignaturaElectiva();
        electiva.setId(id);
        electiva.setCupoMaximo(cupo);
        electiva.setActive(true);
        return electiva;
    }

    @Test
    void rechazaInscripcionEnAsignaturaNoElectiva() {
        AsignaturaBasica basica = new AsignaturaBasica();
        basica.setId(1L);
        basica.setActive(true);
        when(asignaturaRepository.findByIdAndActiveTrueForUpdate(1L)).thenReturn(Optional.of(basica));

        assertThrows(BusinessException.class,
                () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 1L)));
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void rechazaInscripcionSinCupos() {
        when(asignaturaRepository.findByIdAndActiveTrueForUpdate(5L)).thenReturn(Optional.of(electiva(5L, 1)));
        when(inscripcionRepository.findByIdAlumnoAndAsignaturaId(1L, 5L)).thenReturn(Optional.empty());
        when(inscripcionRepository.countByAsignaturaIdAndEstadoIn(eq(5L), anyList())).thenReturn(1);

        assertThrows(BusinessException.class,
                () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 5L)));
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void registraInscripcionCuandoHayCupo() {
        when(asignaturaRepository.findByIdAndActiveTrueForUpdate(5L)).thenReturn(Optional.of(electiva(5L, 2)));
        when(inscripcionRepository.findByIdAlumnoAndAsignaturaId(1L, 5L)).thenReturn(Optional.empty());
        when(inscripcionRepository.countByAsignaturaIdAndEstadoIn(eq(5L), anyList())).thenReturn(0);

        Inscripcion nueva = new Inscripcion();
        when(inscripcionMapper.toEntity(any())).thenReturn(nueva);
        when(inscripcionRepository.save(nueva)).thenReturn(nueva);

        service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 5L));

        assertEquals(EstadoInscripcion.PRE_INSCRITO, nueva.getEstado());
        assertEquals(5L, nueva.getAsignatura().getId());
        verify(inscripcionRepository).save(nueva);
    }

    @Test
    void reactivaInscripcionCancelada() {
        when(asignaturaRepository.findByIdAndActiveTrueForUpdate(5L)).thenReturn(Optional.of(electiva(5L, 2)));
        Inscripcion cancelada = new Inscripcion();
        cancelada.setId(9L);
        cancelada.setEstado(EstadoInscripcion.CANCELADO);
        when(inscripcionRepository.findByIdAlumnoAndAsignaturaId(1L, 5L)).thenReturn(Optional.of(cancelada));
        when(inscripcionRepository.countByAsignaturaIdAndEstadoIn(eq(5L), anyList())).thenReturn(0);
        when(inscripcionRepository.save(cancelada)).thenReturn(cancelada);

        service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 5L));

        assertEquals(EstadoInscripcion.PRE_INSCRITO, cancelada.getEstado());
    }
}

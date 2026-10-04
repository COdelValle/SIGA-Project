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

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.inscripcion.RegistrarInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msasignaturas.client.EstudianteClient;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;
import cl.siga.msasignaturas.model.mapper.InscripcionMapper;
import cl.siga.msasignaturas.repository.InscripcionRepository;
import cl.siga.msasignaturas.repository.asignatura.CursoAsignaturaRepository;

class InscripcionServiceTest {

    private InscripcionRepository inscripcionRepository;
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    private InscripcionMapper inscripcionMapper;
    private InscripcionService service;

    @BeforeEach
    void setUp() {
        inscripcionRepository = mock(InscripcionRepository.class);
        cursoAsignaturaRepository = mock(CursoAsignaturaRepository.class);
        inscripcionMapper = mock(InscripcionMapper.class);
        service = new InscripcionService(
                inscripcionRepository,
                cursoAsignaturaRepository,
                inscripcionMapper,
                mock(EstudianteClient.class));
    }

    private CursoAsignatura dictacion(Long id, CaracterAsignatura caracter, Integer cupo) {
        CursoAsignatura curso = new CursoAsignatura();
        curso.setId(id);
        curso.setCaracter(caracter);
        curso.setCupoMaximo(cupo);
        curso.setActive(true);
        return curso;
    }

    @Test
    void rechazaInscripcionEnAsignaturaObligatoria() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(1L))
                .thenReturn(Optional.of(dictacion(1L, CaracterAsignatura.OBLIGATORIA, null)));

        assertThrows(BusinessException.class,
                () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 1L)));
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void rechazaInscripcionSinCupos() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L))
                .thenReturn(Optional.of(dictacion(5L, CaracterAsignatura.ELECTIVA, 1)));
        when(inscripcionRepository.findByIdAlumnoAndCursoAsignaturaId(1L, 5L)).thenReturn(Optional.empty());
        when(inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(eq(5L), anyList())).thenReturn(1);

        assertThrows(BusinessException.class,
                () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 5L)));
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void registraInscripcionCuandoHayCupo() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L))
                .thenReturn(Optional.of(dictacion(5L, CaracterAsignatura.ELECTIVA, 2)));
        when(inscripcionRepository.findByIdAlumnoAndCursoAsignaturaId(1L, 5L)).thenReturn(Optional.empty());
        when(inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(eq(5L), anyList())).thenReturn(0);

        Inscripcion nueva = new Inscripcion();
        when(inscripcionMapper.toEntity(any())).thenReturn(nueva);
        when(inscripcionRepository.save(nueva)).thenReturn(nueva);

        service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 5L));

        assertEquals(EstadoInscripcion.PRE_INSCRITO, nueva.getEstado());
        assertEquals(5L, nueva.getCursoAsignatura().getId());
        verify(inscripcionRepository).save(nueva);
    }

    @Test
    void reactivaInscripcionCancelada() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L))
                .thenReturn(Optional.of(dictacion(5L, CaracterAsignatura.OPTATIVA, 2)));
        Inscripcion cancelada = new Inscripcion();
        cancelada.setId(9L);
        cancelada.setEstado(EstadoInscripcion.CANCELADO);
        when(inscripcionRepository.findByIdAlumnoAndCursoAsignaturaId(1L, 5L)).thenReturn(Optional.of(cancelada));
        when(inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(eq(5L), anyList())).thenReturn(0);
        when(inscripcionRepository.save(cancelada)).thenReturn(cancelada);

        service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 5L));

        assertEquals(EstadoInscripcion.PRE_INSCRITO, cancelada.getEstado());
    }
}

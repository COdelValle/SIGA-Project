package cl.siga.msasignaturas.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msasignaturas.model.entity.Horario;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaBasica;
import cl.siga.msasignaturas.model.mapper.HorarioMapper;
import cl.siga.msasignaturas.repository.HorarioRepository;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;

class HorarioServiceTest {

    private HorarioRepository horarioRepository;
    private AsignaturaRepository asignaturaRepository;
    private HorarioMapper horarioMapper;
    private HorarioService service;

    @BeforeEach
    void setUp() {
        horarioRepository = mock(HorarioRepository.class);
        asignaturaRepository = mock(AsignaturaRepository.class);
        horarioMapper = mock(HorarioMapper.class);
        service = new HorarioService(horarioRepository, asignaturaRepository, horarioMapper);
    }

    private Horario existente(DiaSemana dia, LocalTime entrada, LocalTime salida) {
        Horario horario = new Horario();
        horario.setId(1L);
        horario.setDia(dia);
        horario.setHorarioEntrada(entrada);
        horario.setHorarioSalida(salida);
        horario.setActive(true);
        return horario;
    }

    private void asignaturaActiva() {
        AsignaturaBasica asignatura = new AsignaturaBasica();
        asignatura.setId(1L);
        asignatura.setActive(true);
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura));
    }

    @Test
    void rechazaHorarioQueSeSolapaEnLaMismaAsignatura() {
        asignaturaActiva();
        when(horarioRepository.findByAsignaturaIdAndActiveTrue(1L))
                .thenReturn(List.of(existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30))));
        when(horarioRepository.findByUbicacionIgnoreCaseAndDiaAndActiveTrue(anyString(), any())).thenReturn(List.of());

        HorarioRequestDTO request = new HorarioRequestDTO(
                DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(10, 0), "SALA 101");

        assertThrows(BusinessException.class, () -> service.crearHorario(1L, request));
        verify(horarioRepository, never()).save(any());
    }

    @Test
    void rechazaHorarioQueOcupaUnaUbicacionEnUso() {
        asignaturaActiva();
        when(horarioRepository.findByAsignaturaIdAndActiveTrue(1L)).thenReturn(List.of());
        when(horarioRepository.findByUbicacionIgnoreCaseAndDiaAndActiveTrue(anyString(), any()))
                .thenReturn(List.of(existente(DiaSemana.MARTES, LocalTime.of(8, 0), LocalTime.of(9, 30))));

        HorarioRequestDTO request = new HorarioRequestDTO(
                DiaSemana.MARTES, LocalTime.of(8, 30), LocalTime.of(9, 0), "SALA 101");

        assertThrows(BusinessException.class, () -> service.crearHorario(1L, request));
        verify(horarioRepository, never()).save(any());
    }

    @Test
    void creaHorarioSinSolapamiento() {
        asignaturaActiva();
        when(horarioRepository.findByAsignaturaIdAndActiveTrue(1L))
                .thenReturn(List.of(existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30))));
        when(horarioRepository.findByUbicacionIgnoreCaseAndDiaAndActiveTrue(anyString(), any())).thenReturn(List.of());

        HorarioRequestDTO request = new HorarioRequestDTO(
                DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(11, 0), "SALA 102");
        Horario nuevo = new Horario();
        when(horarioMapper.toEntity(request)).thenReturn(nuevo);
        when(horarioRepository.save(nuevo)).thenReturn(nuevo);

        service.crearHorario(1L, request);

        assertNotNull(nuevo.getAsignatura());
        assertTrue(nuevo.isActive());
        verify(horarioRepository).save(nuevo);
    }

    @Test
    void noEliminaElUltimoHorarioDeLaAsignatura() {
        Horario unico = existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30));
        AsignaturaBasica asignatura = new AsignaturaBasica();
        asignatura.setId(1L);
        unico.setAsignatura(asignatura);
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(unico));
        when(horarioRepository.countByAsignaturaIdAndActiveTrue(1L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.eliminarHorario(1L));
        verify(horarioRepository, never()).save(any());
    }

    @Test
    void eliminaLogicamenteCuandoHayMasDeUnHorario() {
        Horario horario = existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30));
        AsignaturaBasica asignatura = new AsignaturaBasica();
        asignatura.setId(1L);
        horario.setAsignatura(asignatura);
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(horarioRepository.countByAsignaturaIdAndActiveTrue(1L)).thenReturn(2L);

        service.eliminarHorario(1L);

        assertFalse(horario.isActive());
        verify(horarioRepository).save(horario);
    }

    @Test
    void eliminarUnHorarioYaInactivoEsIdempotente() {
        Horario horario = existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30));
        horario.setActive(false);
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));

        service.eliminarHorario(1L);

        verify(horarioRepository, never()).save(any());
    }
}

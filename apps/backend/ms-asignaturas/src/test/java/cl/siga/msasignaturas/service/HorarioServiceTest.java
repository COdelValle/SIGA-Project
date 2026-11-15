package cl.siga.msasignaturas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.model.entity.Horario;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;
import cl.siga.msasignaturas.model.mapper.HorarioMapper;
import cl.siga.msasignaturas.repository.HorarioRepository;
import cl.siga.msasignaturas.repository.asignatura.CursoAsignaturaRepository;

class HorarioServiceTest {

    private HorarioRepository horarioRepository;
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    private HorarioMapper horarioMapper;
    private HorarioService service;

    @BeforeEach
    void setUp() {
        horarioRepository = mock(HorarioRepository.class);
        cursoAsignaturaRepository = mock(CursoAsignaturaRepository.class);
        horarioMapper = mock(HorarioMapper.class);
        service = new HorarioService(horarioRepository, cursoAsignaturaRepository, horarioMapper);
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

    private void cursoActivo() {
        CursoAsignatura curso = new CursoAsignatura();
        curso.setId(1L);
        curso.setActive(true);
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(curso));
    }

    @Test
    void rechazaHorarioQueSeSolapaEnLaMismaDictacion() {
        cursoActivo();
        when(horarioRepository.findByCursoAsignaturaIdAndActiveTrue(1L))
                .thenReturn(List.of(existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30))));
        when(horarioRepository.findByUbicacionIgnoreCaseAndDiaAndActiveTrue(anyString(), any())).thenReturn(List.of());

        HorarioRequestDTO request = new HorarioRequestDTO(
                DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(10, 0), "SALA 101");

        assertThrows(BusinessException.class, () -> service.crearHorario(1L, request));
        verify(horarioRepository, never()).save(any());
    }

    @Test
    void rechazaHorarioQueOcupaUnaUbicacionEnUso() {
        cursoActivo();
        when(horarioRepository.findByCursoAsignaturaIdAndActiveTrue(1L)).thenReturn(List.of());
        when(horarioRepository.findByUbicacionIgnoreCaseAndDiaAndActiveTrue(anyString(), any()))
                .thenReturn(List.of(existente(DiaSemana.MARTES, LocalTime.of(8, 0), LocalTime.of(9, 30))));

        HorarioRequestDTO request = new HorarioRequestDTO(
                DiaSemana.MARTES, LocalTime.of(8, 30), LocalTime.of(9, 0), "SALA 101");

        assertThrows(BusinessException.class, () -> service.crearHorario(1L, request));
        verify(horarioRepository, never()).save(any());
    }

    @Test
    void creaHorarioSinSolapamiento() {
        cursoActivo();
        when(horarioRepository.findByCursoAsignaturaIdAndActiveTrue(1L))
                .thenReturn(List.of(existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30))));
        when(horarioRepository.findByUbicacionIgnoreCaseAndDiaAndActiveTrue(anyString(), any())).thenReturn(List.of());

        HorarioRequestDTO request = new HorarioRequestDTO(
                DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(11, 0), "SALA 102");
        Horario nuevo = new Horario();
        when(horarioMapper.toEntity(request)).thenReturn(nuevo);
        when(horarioRepository.save(nuevo)).thenReturn(nuevo);

        service.crearHorario(1L, request);

        assertNotNull(nuevo.getCursoAsignatura());
        assertTrue(nuevo.isActive());
        verify(horarioRepository).save(nuevo);
    }

    @Test
    void noEliminaElUltimoHorarioDeLaDictacion() {
        Horario unico = existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30));
        CursoAsignatura curso = new CursoAsignatura();
        curso.setId(1L);
        unico.setCursoAsignatura(curso);
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(unico));
        when(horarioRepository.countByCursoAsignaturaIdAndActiveTrue(1L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.eliminarHorario(1L));
        verify(horarioRepository, never()).save(any());
    }

    @Test
    void eliminaLogicamenteCuandoHayMasDeUnHorario() {
        Horario horario = existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30));
        CursoAsignatura curso = new CursoAsignatura();
        curso.setId(1L);
        horario.setCursoAsignatura(curso);
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(horarioRepository.countByCursoAsignaturaIdAndActiveTrue(1L)).thenReturn(2L);

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

    @Test
    void rechazaCrearHorarioConRangoInvalido() {
        HorarioRequestDTO request = new HorarioRequestDTO(
            DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(9, 0), "SALA 101");

        assertThrows(BusinessException.class, () -> service.crearHorario(1L, request));
        verify(horarioRepository, never()).save(any());
    }

    @Test
    void rechazaActualizarHorarioConRangoInvalido() {
        HorarioRequestDTO request = new HorarioRequestDTO(
            DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(10, 0), "SALA 101");

        assertThrows(BusinessException.class, () -> service.actualizarHorario(1L, request));
    }

    @Test
    void actualizarHorarioExcluyeElMismoHorarioDelSolapamiento() {
        Horario horario = existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30));
        CursoAsignatura curso = new CursoAsignatura();
        curso.setId(1L);
        horario.setCursoAsignatura(curso);
        HorarioRequestDTO request = new HorarioRequestDTO(
            DiaSemana.LUNES, LocalTime.of(8, 30), LocalTime.of(9, 30), "SALA 101");
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(curso));
        when(horarioRepository.findByCursoAsignaturaIdAndActiveTrue(1L)).thenReturn(List.of(horario));
        when(horarioRepository.findByUbicacionIgnoreCaseAndDiaAndActiveTrue("SALA 101", DiaSemana.LUNES))
            .thenReturn(List.of(horario));
        when(horarioRepository.save(horario)).thenReturn(horario);
        when(horarioMapper.toDto(horario)).thenReturn(new HorarioResponseDTO(
            1L, DiaSemana.LUNES, LocalTime.of(8, 30), LocalTime.of(9, 30), "SALA 101"));

        var respuesta = service.actualizarHorario(1L, request);

        assertEquals(DiaSemana.LUNES, respuesta.dia());
        verify(horarioMapper).updateEntityFromDto(request, horario);
        verify(horarioRepository).save(horario);
    }

    @Test
    void actualizarHorarioFallaCuandoNoExisteOEstaInactivo() {
        when(horarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.actualizarHorario(99L, new HorarioRequestDTO(
                DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), "SALA 101")));
    }

    @Test
    void actualizarHorarioFallaCuandoLaDictacionEstaInactiva() {
        Horario horario = existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30));
        CursoAsignatura curso = new CursoAsignatura();
        curso.setId(1L);
        horario.setCursoAsignatura(curso);
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.actualizarHorario(1L, new HorarioRequestDTO(
                DiaSemana.MARTES, LocalTime.of(10, 0), LocalTime.of(11, 0), "SALA 102")));
    }

    @Test
    void actualizarHorarioRechazaSolapamientoConOtroHorario() {
        Horario horario = existente(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30));
        CursoAsignatura curso = new CursoAsignatura();
        curso.setId(1L);
        horario.setCursoAsignatura(curso);
        Horario otro = existente(DiaSemana.LUNES, LocalTime.of(8, 30), LocalTime.of(9, 30));
        otro.setId(2L);
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(curso));
        when(horarioRepository.findByCursoAsignaturaIdAndActiveTrue(1L)).thenReturn(List.of(otro));
        when(horarioRepository.findByUbicacionIgnoreCaseAndDiaAndActiveTrue(anyString(), any()))
            .thenReturn(List.of());

        assertThrows(BusinessException.class,
            () -> service.actualizarHorario(1L, new HorarioRequestDTO(
                DiaSemana.LUNES, LocalTime.of(8, 15), LocalTime.of(9, 0), "SALA 101")));
    }
}

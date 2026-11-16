package cl.siga.msasignaturas.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.msasignaturas.model.entity.Horario;

class HorarioMapperImplTest {

    private final HorarioMapperImpl mapper = new HorarioMapperImpl();

    @Test
    void toEntityMapeaElRequestYDevuelveNullConEntradaNula() {
        HorarioRequestDTO request = new HorarioRequestDTO(
            DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30), "Sala 101");

        Horario horario = mapper.toEntity(request);

        assertEquals(DiaSemana.LUNES, horario.getDia());
        assertEquals(LocalTime.of(8, 0), horario.getHorarioEntrada());
        assertEquals(LocalTime.of(9, 30), horario.getHorarioSalida());
        assertEquals("Sala 101", horario.getUbicacion());
        assertNull(mapper.toEntity(null));
    }

    @Test
    void updateEntityIgnoraNulosYActualizaLosCamposPresentes() {
        Horario horario = Horario.builder()
            .dia(DiaSemana.LUNES)
            .horarioEntrada(LocalTime.of(8, 0))
            .horarioSalida(LocalTime.of(9, 30))
            .ubicacion("Sala 101")
            .build();

        mapper.updateEntityFromDto(
            new HorarioRequestDTO(DiaSemana.MARTES, LocalTime.of(10, 0), LocalTime.of(11, 0), "Sala 102"),
            horario);

        assertEquals(DiaSemana.MARTES, horario.getDia());
        assertEquals(LocalTime.of(10, 0), horario.getHorarioEntrada());
        assertEquals(LocalTime.of(11, 0), horario.getHorarioSalida());
        assertEquals("Sala 102", horario.getUbicacion());

        mapper.updateEntityFromDto(new HorarioRequestDTO(null, null, null, null), horario);

        assertEquals(DiaSemana.MARTES, horario.getDia());
        assertEquals("Sala 102", horario.getUbicacion());

        mapper.updateEntityFromDto(null, horario);

        assertEquals(DiaSemana.MARTES, horario.getDia());
    }

    @Test
    void toDtoMapeaElHorarioYDevuelveNullConEntidadNula() {
        Horario horario = Horario.builder()
            .id(1L)
            .dia(DiaSemana.VIERNES)
            .horarioEntrada(LocalTime.of(12, 15))
            .horarioSalida(LocalTime.of(13, 0))
            .ubicacion("Laboratorio")
            .build();

        HorarioResponseDTO dto = mapper.toDto(horario);

        assertEquals(1L, dto.id());
        assertEquals(DiaSemana.VIERNES, dto.dia());
        assertEquals(LocalTime.of(12, 15), dto.horarioEntrada());
        assertEquals(LocalTime.of(13, 0), dto.horarioSalida());
        assertEquals("Laboratorio", dto.ubicacion());
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDtoListMapeaLaColeccionYDevuelveNullConListaNula() {
        Horario horario = Horario.builder().id(1L).dia(DiaSemana.LUNES).build();

        List<HorarioResponseDTO> dtos = mapper.toDtoList(List.of(horario));

        assertEquals(1, dtos.size());
        assertEquals(1L, dtos.get(0).id());
        assertNull(mapper.toDtoList(null));
        assertTrue(mapper.toDtoList(List.of()).isEmpty());
    }
}

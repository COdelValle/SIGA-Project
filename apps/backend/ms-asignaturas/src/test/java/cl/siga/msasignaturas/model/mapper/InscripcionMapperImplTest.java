package cl.siga.msasignaturas.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.RegistrarInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;

class InscripcionMapperImplTest {

    private final InscripcionMapperImpl mapper = new InscripcionMapperImpl();

    @Test
    void toDtoMapeaLaInscripcionCompleta() {
        CursoAsignatura curso = new CursoAsignatura();
        curso.setId(5L);
        Inscripcion inscripcion = Inscripcion.builder()
            .id(1L)
            .idAlumno(10L)
            .cursoAsignatura(curso)
            .estado(EstadoInscripcion.ACTIVO)
            .fechaInscripcion(LocalDateTime.of(2026, 3, 1, 9, 30))
            .build();

        InscripcionResponseDTO dto = mapper.toDto(inscripcion);

        assertEquals(1L, dto.id());
        assertEquals(10L, dto.idAlumno());
        assertEquals(5L, dto.idCursoAsignatura());
        assertEquals(EstadoInscripcion.ACTIVO, dto.estado());
        assertEquals(LocalDateTime.of(2026, 3, 1, 9, 30), dto.fechaInscripcion());
    }

    @Test
    void toDtoToleraCursoNuloYSinIdentificador() {
        Inscripcion sinCurso = Inscripcion.builder().id(1L).idAlumno(10L).build();
        Inscripcion cursoSinId = Inscripcion.builder()
            .id(2L)
            .idAlumno(11L)
            .cursoAsignatura(new CursoAsignatura())
            .build();

        assertNull(mapper.toDto(sinCurso).idCursoAsignatura());
        assertNull(mapper.toDto(cursoSinId).idCursoAsignatura());
        assertNull(mapper.toDto(null));
    }

    @Test
    void toEntityMapeaSoloElAlumnoYDevuelveNullConRequestNulo() {
        Inscripcion inscripcion = mapper.toEntity(new RegistrarInscripcionRequestDTO(10L, 5L));

        assertEquals(10L, inscripcion.getIdAlumno());
        assertNull(inscripcion.getId());
        assertNull(inscripcion.getCursoAsignatura());
        assertNull(inscripcion.getEstado());
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toDtoListMapeaLaColeccionYDevuelveNullConListaNula() {
        List<InscripcionResponseDTO> dtos = mapper.toDtoList(
            List.of(Inscripcion.builder().id(1L).idAlumno(10L).build()));

        assertEquals(1, dtos.size());
        assertEquals(1L, dtos.get(0).id());
        assertNull(mapper.toDtoList(null));
    }
}

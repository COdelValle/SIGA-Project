package cl.siga.msasignaturas.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.curso.ActualizarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.curso.RegistrarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.model.entity.Horario;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;

class CursoAsignaturaMapperTest {

    private final CursoAsignaturaMapper mapper = new CursoAsignaturaMapper();

    private Asignatura asignatura() {
        return Asignatura.builder()
                .id(1L).nombre("Matemática").descripcion("Básica")
                .area(AreaAcademica.MATEMATICAS).calificable(true).active(true).build();
    }

    private CursoAsignatura curso(Integer cupo) {
        return CursoAsignatura.builder()
                .id(10L).asignatura(asignatura()).idClase(5L).idDocente(2L)
                .semestre(Semestre.SEMESTRE_1).caracter(CaracterAsignatura.OBLIGATORIA)
                .cupoMaximo(cupo).active(true)
                .horarios(new ArrayList<>(List.of(
                        Horario.builder().id(1L).dia(DiaSemana.LUNES)
                                .horarioEntrada(LocalTime.of(8, 0))
                                .horarioSalida(LocalTime.of(9, 30)).ubicacion("Sala 1").active(true).build(),
                        Horario.builder().id(2L).dia(DiaSemana.MARTES)
                                .horarioEntrada(LocalTime.of(10, 0))
                                .horarioSalida(LocalTime.of(11, 30)).ubicacion("Sala 2").active(false).build())))
                .inscripciones(new ArrayList<>(List.of(
                        Inscripcion.builder().id(1L).estado(EstadoInscripcion.ACTIVO).build(),
                        Inscripcion.builder().id(2L).estado(EstadoInscripcion.PRE_INSCRITO).build(),
                        Inscripcion.builder().id(3L).estado(EstadoInscripcion.CANCELADO).build())))
                .build();
    }

    @Test
    void toDtoMapeaCamposCuposEInscritos() {
        CursoAsignaturaResponseDTO dto = mapper.toDto(curso(10));

        assertEquals(10L, dto.id());
        assertEquals(1L, dto.idAsignatura());
        assertEquals("Matemática", dto.nombre());
        assertEquals(AreaAcademica.MATEMATICAS, dto.area());
        assertEquals(10, dto.cupoMaximo());
        assertEquals(8, dto.cuposDisponibles());
        assertEquals(2, dto.totalInscritos());
        assertEquals(1, dto.horarios().size());
        assertEquals(DiaSemana.LUNES, dto.horarios().get(0).dia());
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDtoSinCupoNiListasDevuelveNulos() {
        CursoAsignatura curso = CursoAsignatura.builder().id(10L).asignatura(asignatura())
                .horarios(null).inscripciones(null).build();

        CursoAsignaturaResponseDTO dto = mapper.toDto(curso);

        assertNull(dto.cuposDisponibles());
        assertEquals(0, dto.totalInscritos());
        assertTrue(dto.horarios().isEmpty());
    }

    @Test
    void toDtoConCupoMenorQueInscritosNoBajaDeCero() {
        assertEquals(0, mapper.toDto(curso(1)).cuposDisponibles());
    }

    @Test
    void toEntityMapeaCamposYHorarios() {
        RegistrarCursoAsignaturaRequestDTO dto = new RegistrarCursoAsignaturaRequestDTO(
                1L, 5L, 2L, Semestre.SEMESTRE_1, CaracterAsignatura.OPTATIVA, 15,
                List.of(new HorarioRequestDTO(DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(9, 30), "Sala 1")));

        CursoAsignatura entity = mapper.toEntity(dto, asignatura());

        assertEquals(5L, entity.getIdClase());
        assertEquals(2L, entity.getIdDocente());
        assertEquals(Semestre.SEMESTRE_1, entity.getSemestre());
        assertEquals(CaracterAsignatura.OPTATIVA, entity.getCaracter());
        assertEquals(15, entity.getCupoMaximo());
        assertEquals(1, entity.getHorarios().size());
        assertTrue(entity.getHorarios().get(0).isActive());
        assertEquals("Sala 1", entity.getHorarios().get(0).getUbicacion());
    }

    @Test
    void toEntitySinHorariosDevuelveListaVacia() {
        RegistrarCursoAsignaturaRequestDTO dto = new RegistrarCursoAsignaturaRequestDTO(
                1L, 5L, 2L, Semestre.SEMESTRE_1, CaracterAsignatura.OBLIGATORIA, null, null);

        assertTrue(mapper.toEntity(dto, asignatura()).getHorarios().isEmpty());
    }

    @Test
    void updateEntityActualizaTodosLosCampos() {
        CursoAsignatura entity = curso(10);
        mapper.updateEntity(new ActualizarCursoAsignaturaRequestDTO(
                3L, Semestre.SEMESTRE_2, CaracterAsignatura.ELECTIVA, 20), entity);

        assertEquals(3L, entity.getIdDocente());
        assertEquals(Semestre.SEMESTRE_2, entity.getSemestre());
        assertEquals(CaracterAsignatura.ELECTIVA, entity.getCaracter());
        assertEquals(20, entity.getCupoMaximo());
    }
}

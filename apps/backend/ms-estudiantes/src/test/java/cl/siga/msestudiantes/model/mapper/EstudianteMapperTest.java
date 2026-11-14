package cl.siga.msestudiantes.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.enums.State;
import cl.siga.msestudiantes.model.entity.Estudiante;

class EstudianteMapperTest {

    private final EstudianteMapper mapper = new EstudianteMapperImpl();

    private RegistrarEstudianteRequestDTO request() {
        return new RegistrarEstudianteRequestDTO("oid-1", "Ana", "Maria", "Perez", "Soto",
                "21.000.001-1", LocalDate.of(2012, 5, 1), List.of("Maní"), 7L);
    }

    private Estudiante estudiante() {
        return Estudiante.builder()
                .id(1L)
                .idUsuario("oid-1")
                .rut("21000001-1")
                .firstName("Ana")
                .middleName("Maria")
                .firstSurname("Perez")
                .secondSurname("Soto")
                .birthDate(LocalDate.of(2012, 5, 1))
                .allergies(new ArrayList<>(List.of("Maní")))
                .state(State.REGISTRADO)
                .idClase(7L)
                .build();
    }

    @Test
    void toEntityMapeaTodosLosCampos() {
        Estudiante entity = mapper.toEntity(request());

        assertEquals("oid-1", entity.getIdUsuario());
        assertEquals("21.000.001-1", entity.getRut());
        assertEquals("Ana", entity.getFirstName());
        assertEquals("Maria", entity.getMiddleName());
        assertEquals("Perez", entity.getFirstSurname());
        assertEquals("Soto", entity.getSecondSurname());
        assertEquals(LocalDate.of(2012, 5, 1), entity.getBirthDate());
        assertEquals(List.of("Maní"), entity.getAllergies());
        assertEquals(7L, entity.getIdClase());
    }

    @Test
    void toEntityNuloDevuelveNulo() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void updateEntityFromDtoIgnoraValoresNulos() {
        Estudiante entity = estudiante();
        mapper.updateEntityFromDto(new ActualizarEstudianteRequestDTO(
                null, null, null, null, null, null, null), entity);

        assertEquals("Ana", entity.getFirstName());
        assertEquals("Perez", entity.getFirstSurname());
        assertEquals(List.of("Maní"), entity.getAllergies());
    }

    @Test
    void updateEntityFromDtoConDtoNuloNoCambiaLaEntidad() {
        Estudiante entity = estudiante();
        mapper.updateEntityFromDto(null, entity);

        assertEquals("Ana", entity.getFirstName());
    }

    @Test
    void updateEntityFromDtoReemplazaLosCamposInformados() {
        Estudiante entity = estudiante();
        mapper.updateEntityFromDto(new ActualizarEstudianteRequestDTO(
                "Ana Maria", "Josefa", "Perez", "Soto",
                LocalDate.of(2013, 6, 2), List.of("Polen"), 9L), entity);

        assertEquals("Ana Maria", entity.getFirstName());
        assertEquals("Josefa", entity.getMiddleName());
        assertEquals(LocalDate.of(2013, 6, 2), entity.getBirthDate());
        assertEquals(List.of("Polen"), entity.getAllergies());
        assertEquals(9L, entity.getIdClase());
    }

    @Test
    void updateEntityFromDtoAsignaAlergiasSiLaEntidadNoTieneLista() {
        Estudiante entity = Estudiante.builder().id(1L).firstName("Ana").build();
        mapper.updateEntityFromDto(new ActualizarEstudianteRequestDTO(
                "Ana", null, "Perez", null, LocalDate.of(2012, 5, 1),
                List.of("Polen"), null), entity);

        assertEquals(List.of("Polen"), entity.getAllergies());
    }

    @Test
    void toResponseDtoMapeaEstadoYAlergias() {
        EstudianteResponseDTO dto = mapper.toResponseDto(estudiante());

        assertEquals(1L, dto.id());
        assertEquals("oid-1", dto.idUsuario());
        assertEquals("REGISTRADO", dto.state());
        assertEquals(List.of("Maní"), dto.allergies());
        assertEquals(7L, dto.idClase());
    }

    @Test
    void toResponseDtoSinEstadoNiAlergiasDevuelveNulos() {
        EstudianteResponseDTO dto = mapper.toResponseDto(Estudiante.builder().id(1L).build());

        assertNull(dto.state());
        assertNull(dto.allergies());
    }

    @Test
    void toResponseDtoNuloDevuelveNulo() {
        assertNull(mapper.toResponseDto(null));
    }

    @Test
    void toResponseDtoListMapeaLaLista() {
        assertEquals(1, mapper.toResponseDtoList(List.of(estudiante())).size());
        assertNull(mapper.toResponseDtoList(null));
    }
}

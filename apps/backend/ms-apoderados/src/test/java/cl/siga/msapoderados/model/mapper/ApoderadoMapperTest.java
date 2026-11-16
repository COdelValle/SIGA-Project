package cl.siga.msapoderados.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.apoderado.ActualizarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.RegistrarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.msapoderados.model.entity.Apoderado;
import cl.siga.msapoderados.model.entity.ApoderadoEstudiante;

class ApoderadoMapperTest {

    private final ApoderadoMapper mapper = new ApoderadoMapperImpl();

    private RegistrarApoderadoRequestDTO request() {
        return new RegistrarApoderadoRequestDTO("oid-1", "Luis", "Alberto", "Soto", "Perez",
                "21.000.003-8", List.of("+56912345678"),
                List.of(new ParentescoEstudianteDTO(7L, Parentesco.MADRE_PADRE)));
    }

    private Apoderado apoderado() {
        return Apoderado.builder()
                .id(3L)
                .idUsuario("oid-1")
                .rut("21000003-8")
                .firstName("Luis")
                .middleName("Alberto")
                .firstSurname("Soto")
                .secondSurname("Perez")
                .telefonos(new ArrayList<>(List.of("+56912345678")))
                .estudiantes(new ArrayList<>(List.of(
                        ApoderadoEstudiante.builder().idEstudiante(7L)
                                .parentesco(Parentesco.MADRE_PADRE).build())))
                .activo(true)
                .build();
    }

    @Test
    void toEntityMapeaTodosLosCamposYVinculos() {
        Apoderado entity = mapper.toEntity(request());

        assertEquals("oid-1", entity.getIdUsuario());
        assertEquals("21.000.003-8", entity.getRut());
        assertEquals("Luis", entity.getFirstName());
        assertEquals(List.of("+56912345678"), entity.getTelefonos());
        assertEquals(1, entity.getEstudiantes().size());
        assertEquals(7L, entity.getEstudiantes().get(0).getIdEstudiante());
        assertEquals(Parentesco.MADRE_PADRE, entity.getEstudiantes().get(0).getParentesco());
    }

    @Test
    void toEntityNuloDevuelveNulo() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void updateEntityFromDtoIgnoraValoresNulos() {
        Apoderado entity = apoderado();
        mapper.updateEntityFromDto(new ActualizarApoderadoRequestDTO(
                null, null, null, null, null), entity);

        assertEquals("Luis", entity.getFirstName());
        assertEquals(List.of("+56912345678"), entity.getTelefonos());
    }

    @Test
    void updateEntityFromDtoConDtoNuloNoCambiaLaEntidad() {
        Apoderado entity = apoderado();
        mapper.updateEntityFromDto(null, entity);

        assertEquals("Luis", entity.getFirstName());
    }

    @Test
    void updateEntityFromDtoReemplazaTelefonosYNombre() {
        Apoderado entity = apoderado();
        mapper.updateEntityFromDto(new ActualizarApoderadoRequestDTO(
                "Luis Alberto", "Jose", "Soto", "Perez",
                List.of("+56987654321")), entity);

        assertEquals("Luis Alberto", entity.getFirstName());
        assertEquals("Jose", entity.getMiddleName());
        assertEquals(List.of("+56987654321"), entity.getTelefonos());
    }

    @Test
    void updateEntityFromDtoAsignaTelefonosSiLaEntidadNoTieneLista() {
        Apoderado entity = Apoderado.builder().id(3L).firstName("Luis").build();
        mapper.updateEntityFromDto(new ActualizarApoderadoRequestDTO(
                "Luis", null, "Soto", null, List.of("+56987654321")), entity);

        assertEquals(List.of("+56987654321"), entity.getTelefonos());
    }

    @Test
    void toResponseDtoMapeaTelefonosYVinculos() {
        ApoderadoResponseDTO dto = mapper.toResponseDto(apoderado());

        assertEquals(3L, dto.id());
        assertEquals("oid-1", dto.idUsuario());
        assertEquals(List.of("+56912345678"), dto.telefonos());
        assertEquals(1, dto.estudiantes().size());
        assertEquals(Parentesco.MADRE_PADRE, dto.estudiantes().get(0).parentesco());
        assertEquals(Boolean.TRUE, dto.activo());
    }

    @Test
    void toResponseDtoNuloDevuelveNulo() {
        assertNull(mapper.toResponseDto(null));
    }

    @Test
    void toResponseDtoListMapeaLaLista() {
        assertEquals(1, mapper.toResponseDtoList(List.of(apoderado())).size());
        assertNull(mapper.toResponseDtoList(null));
    }

    @Test
    void toApoderadoEstudianteEntityMapeaElVinculo() {
        ApoderadoEstudiante entity = mapper.toApoderadoEstudianteEntity(
                new ParentescoEstudianteDTO(9L, Parentesco.TIA_TIO));

        assertEquals(9L, entity.getIdEstudiante());
        assertEquals(Parentesco.TIA_TIO, entity.getParentesco());
        assertNull(mapper.toApoderadoEstudianteEntity(null));
    }

    @Test
    void toParentescoEstudianteDtoMapeaElVinculo() {
        ParentescoEstudianteDTO dto = mapper.toParentescoEstudianteDto(
                ApoderadoEstudiante.builder().idEstudiante(9L).parentesco(Parentesco.TIA_TIO).build());

        assertEquals(9L, dto.idEstudiante());
        assertEquals(Parentesco.TIA_TIO, dto.parentesco());
        assertNull(mapper.toParentescoEstudianteDto(null));
    }
}

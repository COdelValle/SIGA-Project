package cl.siga.msclases.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.clase.ActualizarDocenteJefeRequestDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.RegistrarClaseRequestDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.msclases.model.entity.Clase;

class ClaseMapperTest {

    private final ClaseMapper mapper = new ClaseMapperImpl();

    private Clase clase() {
        return Clase.builder()
                .id(1L)
                .nivel(Nivel.OCTAVO_BASICO)
                .letra("A")
                .anioAcademico(2026)
                .idDocenteJefe(2L)
                .active(true)
                .build();
    }

    @Test
    void toEntityMapeaLosCampos() {
        Clase entity = mapper.toEntity(new RegistrarClaseRequestDTO(
                Nivel.OCTAVO_BASICO, "A", 2026, 2L));

        assertEquals(Nivel.OCTAVO_BASICO, entity.getNivel());
        assertEquals("A", entity.getLetra());
        assertEquals(2026, entity.getAnioAcademico());
        assertEquals(2L, entity.getIdDocenteJefe());
        assertNull(mapper.toEntity(null));
    }

    @Test
    void updateEntityFromDtoIgnoraNuloYActualizaValor() {
        Clase entity = clase();
        mapper.updateEntityFromDto(new ActualizarDocenteJefeRequestDTO(null), entity);
        assertEquals(2L, entity.getIdDocenteJefe());

        mapper.updateEntityFromDto(new ActualizarDocenteJefeRequestDTO(3L), entity);
        assertEquals(3L, entity.getIdDocenteJefe());

        mapper.updateEntityFromDto(null, entity);
        assertEquals(3L, entity.getIdDocenteJefe());
    }

    @Test
    void toResponseDtoMapeaLosCampos() {
        ClaseResponseDTO dto = mapper.toResponseDto(clase());

        assertEquals(1L, dto.id());
        assertEquals(Nivel.OCTAVO_BASICO, dto.nivel());
        assertEquals("A", dto.letra());
        assertEquals(2026, dto.anioAcademico());
        assertEquals(2L, dto.idDocenteJefe());
        assertNull(mapper.toResponseDto(null));
    }

    @Test
    void toResponseDtoSinIdUsaCero() {
        assertEquals(0L, mapper.toResponseDto(Clase.builder().build()).id());
    }

    @Test
    void toResponseDtoListMapeaLaLista() {
        assertEquals(1, mapper.toResponseDtoList(List.of(clase())).size());
        assertNull(mapper.toResponseDtoList(null));
    }
}

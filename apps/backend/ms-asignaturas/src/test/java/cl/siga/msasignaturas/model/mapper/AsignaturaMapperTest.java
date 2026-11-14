package cl.siga.msasignaturas.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.asignatura.ActualizarAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;

class AsignaturaMapperTest {

    private final AsignaturaMapper mapper = new AsignaturaMapper();

    @Test
    void toDtoMapeaLosCampos() {
        Asignatura asignatura = Asignatura.builder()
                .id(1L).nombre("Matemática").nombreCorto("MAT").descripcion("Básica")
                .area(AreaAcademica.MATEMATICAS).calificable(true).active(true).build();

        AsignaturaResponseDTO dto = mapper.toDto(asignatura);

        assertEquals(1L, dto.id());
        assertEquals("Matemática", dto.nombre());
        assertEquals("MAT", dto.nombreCorto());
        assertEquals(AreaAcademica.MATEMATICAS, dto.area());
        assertTrue(dto.calificable());
        assertTrue(dto.activa());
        assertNull(mapper.toDto(null));
    }

    @Test
    void updateEntityActualizaTodosLosCampos() {
        Asignatura asignatura = Asignatura.builder()
                .id(1L).nombre("Matemática").descripcion("Básica")
                .area(AreaAcademica.MATEMATICAS).calificable(true).active(true).build();

        mapper.updateEntity(new ActualizarAsignaturaRequestDTO(
                "Arte", "ART", "Artes visuales", AreaAcademica.ARTES, false), asignatura);

        assertEquals("Arte", asignatura.getNombre());
        assertEquals("ART", asignatura.getNombreCorto());
        assertEquals("Artes visuales", asignatura.getDescripcion());
        assertEquals(AreaAcademica.ARTES, asignatura.getArea());
        assertFalse(asignatura.isCalificable());
    }
}

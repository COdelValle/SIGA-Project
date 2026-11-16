package cl.siga.msasignaturas.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.entity.asignatura.MallaCurricular;

class MallaCurricularMapperTest {

    private final MallaCurricularMapper mapper = new MallaCurricularMapper();

    @Test
    void toDtoMapeaLosCampos() {
        Asignatura asignatura = Asignatura.builder()
                .id(1L).nombre("Matemática").area(AreaAcademica.MATEMATICAS)
                .calificable(true).active(true).build();
        MallaCurricular malla = MallaCurricular.builder()
                .id(10L).nivel(Nivel.OCTAVO_BASICO).asignatura(asignatura)
                .caracter(CaracterAsignatura.OBLIGATORIA).plan(PlanFormacion.COMUN)
                .horasSemanales(4).active(true).build();

        MallaCurricularResponseDTO dto = mapper.toDto(malla);

        assertEquals(10L, dto.id());
        assertEquals(Nivel.OCTAVO_BASICO, dto.nivel());
        assertEquals(1L, dto.idAsignatura());
        assertEquals("Matemática", dto.nombre());
        assertEquals(AreaAcademica.MATEMATICAS, dto.area());
        assertEquals(CaracterAsignatura.OBLIGATORIA, dto.caracter());
        assertEquals(PlanFormacion.COMUN, dto.plan());
        assertEquals(4, dto.horasSemanales());
        assertTrue(dto.calificable());
        assertTrue(dto.activa());
        assertNull(mapper.toDto(null));
    }
}

package cl.siga.msasignaturas.model.entity.asignatura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AsignaturaTest {

    @Test
    void normalizarRecortaNombreNombreCortoYDescripcion() {
        Asignatura asignatura = Asignatura.builder()
            .nombre("  Matemática  ")
            .nombreCorto("  MAT  ")
            .descripcion("  Asignatura base  ")
            .build();

        asignatura.normalizar();

        assertEquals("Matemática", asignatura.getNombre());
        assertEquals("MAT", asignatura.getNombreCorto());
        assertEquals("Asignatura base", asignatura.getDescripcion());
    }

    @Test
    void normalizarToleraCamposNulos() {
        Asignatura asignatura = new Asignatura();

        asignatura.normalizar();

        assertNull(asignatura.getNombre());
        assertNull(asignatura.getNombreCorto());
        assertNull(asignatura.getDescripcion());
    }
}

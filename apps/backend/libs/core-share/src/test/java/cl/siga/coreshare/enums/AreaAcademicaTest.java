package cl.siga.coreshare.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AreaAcademicaTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void todasLasAreasTienenNombreVisibleYNoSeRepiten() {
        assertThat(AreaAcademica.values()).hasSize(15);
        assertThat(AreaAcademica.values()).allSatisfy(area ->
                assertThat(area.getNombre()).isNotBlank());
        assertThat(AreaAcademica.values()).extracting(AreaAcademica::getNombre).doesNotHaveDuplicates();
    }

    @Test
    void serializaConElNombreVisible() throws Exception {
        assertThat(mapper.writeValueAsString(AreaAcademica.MATEMATICAS))
                .isEqualTo("\"Matemáticas\"");
        assertThat(mapper.writeValueAsString(AreaAcademica.EDUCACION_FISICA))
                .isEqualTo("\"Educación Física\"");
    }

    @Test
    void deserializaElNombreVisible() throws Exception {
        assertThat(mapper.readValue("\"Ciencias para la Ciudadanía\"", AreaAcademica.class))
                .isEqualTo(AreaAcademica.CIENCIAS_CIUDADANIA);
        assertThat(mapper.readValue("\"Otra Área\"", AreaAcademica.class))
                .isEqualTo(AreaAcademica.OTRA);
    }
}

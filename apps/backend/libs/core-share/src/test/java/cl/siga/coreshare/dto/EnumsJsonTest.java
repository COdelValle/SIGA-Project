package cl.siga.coreshare.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;

/**
 * Los enums con @JsonValue (texto visible) deben aceptar tambien el nombre de
 * la constante al deserializar, para que el frontend haga round-trip del valor
 * que recibe (p. ej. "SUMATIVA") sin fallar con 400.
 */
class EnumsJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void tipoEvaluacionAceptaNombreYTextoVisible() throws Exception {
        assertThat(mapper.readValue("\"SUMATIVA\"", TipoEvaluacion.class))
            .isEqualTo(TipoEvaluacion.SUMATIVA);
        assertThat(mapper.readValue("\"sumativa\"", TipoEvaluacion.class))
            .isEqualTo(TipoEvaluacion.SUMATIVA);
        assertThat(mapper.readValue("\"Evaluación Sumativa\"", TipoEvaluacion.class))
            .isEqualTo(TipoEvaluacion.SUMATIVA);
        assertThat(mapper.writeValueAsString(TipoEvaluacion.SUMATIVA))
            .isEqualTo("\"Evaluación Sumativa\"");
    }

    @Test
    void diaSemanaAceptaNombreYTextoVisible() throws Exception {
        assertThat(mapper.readValue("\"MIERCOLES\"", DiaSemana.class))
            .isEqualTo(DiaSemana.MIERCOLES);
        assertThat(mapper.readValue("\"Miércoles\"", DiaSemana.class))
            .isEqualTo(DiaSemana.MIERCOLES);
        assertThat(mapper.writeValueAsString(DiaSemana.MIERCOLES)).isEqualTo("\"Miércoles\"");
    }

    @Test
    void nivelAceptaNombreYTextoVisible() throws Exception {
        assertThat(mapper.readValue("\"OCTAVO_BASICO\"", Nivel.class))
            .isEqualTo(Nivel.OCTAVO_BASICO);
        assertThat(mapper.readValue("\"8vo Básico\"", Nivel.class))
            .isEqualTo(Nivel.OCTAVO_BASICO);
        assertThat(mapper.writeValueAsString(Nivel.OCTAVO_BASICO)).isEqualTo("\"8vo Básico\"");
    }

    @Test
    void parentescoAceptaNombreYTextoVisible() throws Exception {
        assertThat(mapper.readValue("\"MADRE_PADRE\"", Parentesco.class))
            .isEqualTo(Parentesco.MADRE_PADRE);
        assertThat(mapper.readValue("\"Madre/Padre\"", Parentesco.class))
            .isEqualTo(Parentesco.MADRE_PADRE);
        assertThat(mapper.writeValueAsString(Parentesco.MADRE_PADRE)).isEqualTo("\"Madre/Padre\"");
    }

    @Test
    void valoresInvalidosFallan() {
        assertThatThrownBy(() -> mapper.readValue("\"NO_EXISTE\"", TipoEvaluacion.class))
            .hasMessageContaining("Tipo de evaluación inválido");
        assertThatThrownBy(() -> mapper.readValue("\"NO_EXISTE\"", Nivel.class))
            .hasMessageContaining("Nivel inválido");
    }
}

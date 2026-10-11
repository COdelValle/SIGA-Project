package cl.siga.coreshare.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;

/**
 * Los enums con @JsonValue (texto visible) deben aceptar tambien el nombre de
 * la constante al deserializar. TipoEvaluacion es la excepcion: serializa el
 * nombre de la constante (el frontend compara "SUMATIVA") y ademas acepta el
 * texto visible legacy al leer.
 */
class EnumsJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void tipoEvaluacionSerializaConstanteYAceptaTextoVisible() throws Exception {
        assertThat(mapper.readValue("\"SUMATIVA\"", TipoEvaluacion.class))
            .isEqualTo(TipoEvaluacion.SUMATIVA);
        assertThat(mapper.readValue("\"sumativa\"", TipoEvaluacion.class))
            .isEqualTo(TipoEvaluacion.SUMATIVA);
        assertThat(mapper.readValue("\"Evaluación Sumativa\"", TipoEvaluacion.class))
            .isEqualTo(TipoEvaluacion.SUMATIVA);
        assertThat(mapper.writeValueAsString(TipoEvaluacion.SUMATIVA))
            .isEqualTo("\"SUMATIVA\"");
    }

    @Test
    void evaluacionResponseSerializaElTipoComoConstante() throws Exception {
        EvaluacionResponseDTO dto = new EvaluacionResponseDTO(
            9L, "PRUEBA 1", TipoEvaluacion.SUMATIVA, 30.0, 3L, true);

        assertThat(mapper.writeValueAsString(dto)).contains("\"tipo\":\"SUMATIVA\"");
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

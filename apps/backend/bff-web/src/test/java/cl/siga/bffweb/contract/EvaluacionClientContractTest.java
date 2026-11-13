package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.evaluaciones.EvaluacionClient;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;

@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.evaluaciones.url=http://localhost:18088"
)
class EvaluacionClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private EvaluacionClient evaluacionClient;

    @Override
    protected String specFile() {
        return "ms-evaluaciones.json";
    }

    @Override
    protected int port() {
        return 18088;
    }

    @Test
    void getEvaluacionByIdCumpleElContratoOpenApi() {
        stubGet("/api/v1/evaluaciones/9",
            """
            {"id":9,"nombre":"Prueba de unidad","tipo":"SUMATIVA","ponderacion":60.0,
             "idCursoAsignatura":3,"active":true}
            """);

        EvaluacionResponseDTO evaluacion = evaluacionClient.getEvaluacionById(9L);

        assertThat(evaluacion).isNotNull();
        assertThat(evaluacion.id()).isEqualTo(9L);
        assertContrato();
    }
}

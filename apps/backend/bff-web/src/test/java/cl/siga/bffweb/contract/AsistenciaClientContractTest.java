package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.asistencias.AsistenciaClient;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;

@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.asistencias.url=http://localhost:18090"
)
class AsistenciaClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private AsistenciaClient asistenciaClient;

    @Override
    protected String specFile() {
        return "ms-asistencias.json";
    }

    @Override
    protected int port() {
        return 18090;
    }

    @Test
    void getAsistenciaByIdCumpleElContratoOpenApi() {
        stubGet("/api/v1/asistencias/1",
            """
            {"id":1,"idEstudiante":1,"idCursoAsignatura":3,"fecha":"2026-03-04",
             "estado":"PRESENTE","justificacion":"NO_APLICA","observacion":"Sin observacion"}
            """);

        AsistenciaResponseDTO asistencia = asistenciaClient.getAsistenciaById(1L);

        assertThat(asistencia).isNotNull();
        assertThat(asistencia.id()).isEqualTo(1L);
        assertContrato();
    }
}

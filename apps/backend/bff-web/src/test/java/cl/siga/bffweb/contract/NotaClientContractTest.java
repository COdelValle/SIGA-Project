package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.notas.NotaClient;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;

@Epic("Contratos BFF")
@Feature("OpenAPI")
@Owner("qa")
@Tag("contract")
@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.notas.url=http://localhost:18079"
)
class NotaClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private NotaClient notaClient;

    @Override
    protected String specFile() {
        return "ms-notas.json";
    }

    @Override
    protected int port() {
        return 18079;
    }

    @Test
    void getNotaByIdCumpleElContratoOpenApi() {
        stubGet("/api/v1/notas/7",
            "{\"id\":7,\"idEstudiante\":1,\"idEvaluacion\":9,\"score\":6.5}");

        NotaResponseDTO nota = notaClient.getNotaById(7L);

        assertThat(nota).isNotNull();
        assertThat(nota.id()).isEqualTo(7L);
        assertThat(nota.score()).isEqualTo(6.5);
        assertContrato();
    }
}

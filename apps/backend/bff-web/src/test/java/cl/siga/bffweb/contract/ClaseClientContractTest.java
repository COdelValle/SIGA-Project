package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;

@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.clases.url=http://localhost:18087"
)
class ClaseClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private ClaseClient claseClient;

    @Override
    protected String specFile() {
        return "ms-clases.json";
    }

    @Override
    protected int port() {
        return 18087;
    }

    @Test
    void getClaseByIdCumpleElContratoOpenApi() {
        stubGet("/api/v1/clases/4",
            "{\"id\":4,\"nivel\":\"8vo Básico\",\"letra\":\"A\",\"anioAcademico\":2026,\"idDocenteJefe\":1}");

        ClaseResponseDTO clase = claseClient.getClaseById(4L);

        assertThat(clase).isNotNull();
        assertThat(clase.id()).isEqualTo(4L);
        assertContrato();
    }
}

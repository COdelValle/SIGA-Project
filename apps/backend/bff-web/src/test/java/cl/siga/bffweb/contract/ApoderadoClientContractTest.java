package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.apoderados.ApoderadoClient;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;

@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.apoderados.url=http://localhost:18084"
)
class ApoderadoClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private ApoderadoClient apoderadoClient;

    @Override
    protected String specFile() {
        return "ms-apoderados.json";
    }

    @Override
    protected int port() {
        return 18084;
    }

    @Test
    void getApoderadoByIdUsuarioCumpleElContratoOpenApi() {
        stubGet("/api/v1/apoderados/idUsuario/u1",
            """
            {"id":2,"idUsuario":"u1","firstName":"Maria","middleName":"Elena","firstSurname":"Diaz",
             "secondSurname":"Rojas","rut":"33.333.333-3","telefonos":["+56911111111"],
             "estudiantes":[],"activo":true}
            """);

        ApoderadoResponseDTO apoderado = paso("GET /api/v1/apoderados/idUsuario/u1",
            () -> apoderadoClient.getApoderadoByIdUsuario("u1"));

        assertThat(apoderado).isNotNull();
        assertThat(apoderado.id()).isEqualTo(2L);
        assertContrato();
    }
}

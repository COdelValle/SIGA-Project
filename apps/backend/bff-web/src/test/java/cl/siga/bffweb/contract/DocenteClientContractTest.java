package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;

@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.docentes.url=http://localhost:18085"
)
class DocenteClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private DocenteClient docenteClient;

    @Override
    protected String specFile() {
        return "ms-docentes.json";
    }

    @Override
    protected int port() {
        return 18085;
    }

    @Test
    void getDocenteByIdCumpleElContratoOpenApi() {
        stubGet("/api/v1/docentes/5",
            """
            {"id":5,"idUsuario":"u5","firstName":"Luis","middleName":null,"firstSurname":"Soto",
             "secondSurname":null,"rut":"22.222.222-2","fechaContratacion":"2020-03-01",
             "activo":true,"area":"Ciencias Naturales y Exactas","certificados":[]}
            """);

        DocenteResponseDTO docente = docenteClient.getDocenteById(5L);

        assertThat(docente).isNotNull();
        assertThat(docente.id()).isEqualTo(5L);
        assertContrato();
    }
}

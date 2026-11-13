package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;

@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.estudiantes.url=http://localhost:18083"
)
class EstudianteClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private EstudianteClient estudianteClient;

    @Override
    protected String specFile() {
        return "ms-estudiantes.json";
    }

    @Override
    protected int port() {
        return 18083;
    }

    @Test
    void getEstudianteByIdCumpleElContratoOpenApi() {
        stubGet("/api/v1/estudiantes/1",
            """
            {"id":1,"idUsuario":"u1","rut":"11.111.111-1","firstName":"Ana","middleName":"Maria",
             "firstSurname":"Perez","secondSurname":"Gomez","birthDate":"2012-03-04",
             "allergies":[],"state":"ACTIVO","idClase":4}
            """);

        EstudianteResponseDTO estudiante = paso("GET /api/v1/estudiantes/1",
            () -> estudianteClient.getEstudianteById(1L));

        assertThat(estudiante).isNotNull();
        assertThat(estudiante.id()).isEqualTo(1L);
        assertContrato();
    }
}

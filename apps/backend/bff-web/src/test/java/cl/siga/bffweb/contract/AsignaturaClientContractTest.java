package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;

@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.asignaturas.url=http://localhost:18086"
)
class AsignaturaClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private AsignaturaClient asignaturaClient;

    @Override
    protected String specFile() {
        return "ms-asignaturas.json";
    }

    @Override
    protected int port() {
        return 18086;
    }

    @Test
    void getCursoAsignaturaByIdCumpleElContratoOpenApi() {
        stubGet("/api/v1/curso-asignaturas/3",
            """
            {"id":3,"idAsignatura":10,"nombre":"Ciencias Naturales","descripcion":"Ciencias",
             "area":"Ciencias Naturales y Exactas","calificable":true,"caracter":"OBLIGATORIA","semestre":"SEMESTRE_1",
             "idDocente":5,"idClase":4,"cupoMaximo":40,"cuposDisponibles":35,
             "totalInscritos":5,"horarios":[]}
            """);

        CursoAsignaturaResponseDTO curso = asignaturaClient.getCursoAsignaturaById(3L);

        assertThat(curso).isNotNull();
        assertThat(curso.id()).isEqualTo(3L);
        assertContrato();
    }
}

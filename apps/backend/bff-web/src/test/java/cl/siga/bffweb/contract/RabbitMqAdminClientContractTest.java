package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.rabbitmq.RabbitMqAdminClient;
import cl.siga.coreshare.dto.rabbitmq.EstadoColaDTO;

@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.rabbitmq-admin.url=http://localhost:18092"
)
class RabbitMqAdminClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private RabbitMqAdminClient rabbitMqAdminClient;

    @Override
    protected String specFile() {
        return "ms-rabbitmq-admin.json";
    }

    @Override
    protected int port() {
        return 18092;
    }

    @Test
    void estadoColaCumpleElContratoOpenApi() {
        stubGet("/api/v1/rabbitmq/queues/cola.prueba",
            "{\"nombre\":\"cola.prueba\",\"mensajes\":0,\"consumidores\":0}");

        EstadoColaDTO estado = paso("GET /api/v1/rabbitmq/queues/cola.prueba",
            () -> rabbitMqAdminClient.estadoCola("cola.prueba"));

        assertThat(estado).isNotNull();
        assertThat(estado.nombre()).isEqualTo("cola.prueba");
        assertContrato();
    }
}

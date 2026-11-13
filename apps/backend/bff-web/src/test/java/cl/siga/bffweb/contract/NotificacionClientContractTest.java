package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.notificaciones.NotificacionClient;
import cl.siga.coreshare.dto.notificaciones.ContadorNotificacionesDTO;

@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.notificaciones.url=http://localhost:18091"
)
class NotificacionClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private NotificacionClient notificacionClient;

    @Override
    protected String specFile() {
        return "ms-notificaciones.json";
    }

    @Override
    protected int port() {
        return 18091;
    }

    @Test
    void contarNoLeidasCumpleElContratoOpenApi() {
        stubGet("/api/v1/notificaciones/me/no-leidas/count", "{\"noLeidas\":2}");

        ContadorNotificacionesDTO contador = paso("GET /api/v1/notificaciones/me/no-leidas/count",
            () -> notificacionClient.contarNoLeidas());

        assertThat(contador).isNotNull();
        assertThat(contador.noLeidas()).isEqualTo(2);
        assertContrato();
    }
}

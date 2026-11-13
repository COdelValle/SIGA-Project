package cl.siga.bffweb.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.bffweb.integration.usuarios.UsuarioClient;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;

@SpringBootTest(
    classes = AbstractFeignContractTest.ContractConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "services.usuarios.url=http://localhost:18081"
)
class UsuarioClientContractTest extends AbstractFeignContractTest {

    @Autowired
    private UsuarioClient usuarioClient;

    @Override
    protected String specFile() {
        return "ms-usuarios-auth.json";
    }

    @Override
    protected int port() {
        return 18081;
    }

    @Test
    void getCurrentUsuarioCumpleElContratoOpenApi() {
        stubGet("/api/v1/usuarios/me",
            "{\"id\":\"u1\",\"fullName\":\"Admin SIGA\",\"email\":\"admin@siga.cl\",\"rol\":\"ADMIN\",\"state\":\"ACTIVO\"}");

        UsuarioResponseDTO usuario = paso("GET /api/v1/usuarios/me",
            () -> usuarioClient.getCurrentUsuario());

        assertThat(usuario).isNotNull();
        assertThat(usuario.id()).isEqualTo("u1");
        assertContrato();
    }
}

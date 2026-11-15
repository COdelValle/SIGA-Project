package cl.siga.bffweb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "AZURE_TENANT_ID=siga-test-tenant",
    "AZURE_CLIENT_ID=siga-test-client",
    "AZURE_APP_ID_URI=api://siga-test-api"
})
class BffWebApplicationTests {

    @Test
    void contextLoads() {
    }

}

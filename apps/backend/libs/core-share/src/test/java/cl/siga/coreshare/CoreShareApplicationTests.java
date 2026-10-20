package cl.siga.coreshare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "AZURE_TENANT_ID=test-tenant",
    "AZURE_CLIENT_ID=test-client",
    "AZURE_APP_ID_URI=api://test-client",
    "spring.rabbitmq.dynamic=false"
})
class CoreShareApplicationTests {

    @Test
    void contextLoads() {
    }

}

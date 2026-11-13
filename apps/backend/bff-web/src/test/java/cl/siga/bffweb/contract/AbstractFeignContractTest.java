package cl.siga.bffweb.contract;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import com.atlassian.oai.validator.wiremock.junit5.OpenApiValidationListener;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import cl.siga.bffweb.integration.apoderados.ApoderadoClient;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.asistencias.AsistenciaClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.bffweb.integration.evaluaciones.EvaluacionClient;
import cl.siga.bffweb.integration.notas.NotaClient;
import cl.siga.bffweb.integration.notificaciones.NotificacionClient;
import cl.siga.bffweb.integration.rabbitmq.RabbitMqAdminClient;
import cl.siga.bffweb.integration.usuarios.UsuarioClient;

public abstract class AbstractFeignContractTest {

    protected WireMockServer server;
    protected OpenApiValidationListener validation;

    protected abstract String specFile();

    protected abstract int port();

    @BeforeEach
    void iniciarWireMock() {
        Path spec = Path.of("..", "..", "..", "docs", "openapi", specFile()).normalize();
        validation = new OpenApiValidationListener(spec.toAbsolutePath().toUri().toString());
        server = new WireMockServer(WireMockConfiguration.options()
            .port(port())
            .extensions(validation));
        server.start();
    }

    @AfterEach
    void detenerWireMock() {
        if (server != null) {
            server.stop();
        }
    }

    protected void stubGet(String path, String json) {
        server.stubFor(get(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(json)));
    }

    protected void assertContrato() {
        validation.assertValidationPassed();
    }

    @SpringBootConfiguration
    @EnableFeignClients(clients = {
        ApoderadoClient.class,
        AsignaturaClient.class,
        AsistenciaClient.class,
        ClaseClient.class,
        DocenteClient.class,
        EstudianteClient.class,
        EvaluacionClient.class,
        NotaClient.class,
        NotificacionClient.class,
        RabbitMqAdminClient.class,
        UsuarioClient.class
    })
    @ComponentScan(basePackages = "cl.siga.bffweb.integration")
    @ImportAutoConfiguration({
        FeignAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class
    })
    static class ContractConfig {
    }
}

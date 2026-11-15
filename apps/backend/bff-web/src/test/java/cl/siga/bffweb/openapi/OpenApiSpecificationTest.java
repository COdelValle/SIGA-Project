package cl.siga.bffweb.openapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.siga.bffweb.domain.admin.AdminBffService;
import cl.siga.bffweb.domain.admin.rabbitmq.RabbitMqAdminBffService;
import cl.siga.bffweb.domain.apoderados.ApoderadoPupiloService;
import cl.siga.bffweb.domain.asistencias.AsistenciaBffService;
import cl.siga.bffweb.domain.docentes.DocenteBffService;
import cl.siga.bffweb.domain.estudiantes.PerfilEstudianteService;
import cl.siga.bffweb.domain.evaluaciones.EvaluacionBffService;
import cl.siga.bffweb.domain.me.MeService;
import cl.siga.bffweb.domain.notas.NotaBffService;
import cl.siga.bffweb.domain.notificaciones.NotificacionBffService;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import({ SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class })
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class OpenApiSpecificationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminBffService adminBffService;

    @MockitoBean
    private RabbitMqAdminBffService rabbitMqAdminBffService;

    @MockitoBean
    private ApoderadoPupiloService apoderadoPupiloService;

    @MockitoBean
    private AsistenciaBffService asistenciaBffService;

    @MockitoBean
    private DocenteBffService docenteBffService;

    @MockitoBean
    private PerfilEstudianteService perfilEstudianteService;

    @MockitoBean
    private EvaluacionBffService evaluacionBffService;

    @MockitoBean
    private MeService meService;

    @MockitoBean
    private NotaBffService notaBffService;

    @MockitoBean
    private NotificacionBffService notificacionBffService;

    @Test
    void generaEspecificacionOpenApi() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Path output = Path.of("..", "..", "..", "docs", "openapi", "bff-web.json").normalize();
        Files.createDirectories(output.getParent());
        Files.writeString(output, body);
    }
}

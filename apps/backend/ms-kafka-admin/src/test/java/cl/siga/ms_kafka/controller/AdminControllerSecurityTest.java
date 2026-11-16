package cl.siga.ms_kafka.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.ms_kafka.service.AdminService;

@WebMvcTest(controllers = AdminController.class)
@ImportAutoConfiguration({SharedSecurityConfig.class, JwtResourceServerConfiguration.class})
@TestPropertySource(properties = {
    "AZURE_TENANT_ID=test-tenant",
    "AZURE_CLIENT_ID=test-client",
    "AZURE_APP_ID_URI=api://test"
})
class AdminControllerSecurityTest {

    private static final SimpleGrantedAuthority ROLE_ADMIN =
        new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final SimpleGrantedAuthority ROLE_USER =
        new SimpleGrantedAuthority("ROLE_USER");
    private static final SimpleGrantedAuthority SCOPE_READ =
        new SimpleGrantedAuthority("SCOPE_usuarios:read");
    private static final SimpleGrantedAuthority SCOPE_DELETE =
        new SimpleGrantedAuthority("SCOPE_usuarios:delete");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/kafka/topics"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioSinRolAdminDevuelve403() throws Exception {
        mockMvc.perform(get("/api/kafka/topics")
                .with(jwt().authorities(ROLE_USER, SCOPE_READ)))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminConScopeReadPuedeListarTopics() throws Exception {
        when(adminService.obtenerTopics()).thenReturn(Set.of());

        mockMvc.perform(get("/api/kafka/topics")
                .with(jwt().authorities(ROLE_ADMIN, SCOPE_READ)))
            .andExpect(status().isOk());
    }

    @Test
    void borrarTopicRequiereScopeDelete() throws Exception {
        mockMvc.perform(delete("/api/kafka/topics/foo")
                .with(jwt().authorities(ROLE_ADMIN, SCOPE_READ)))
            .andExpect(status().isForbidden());

        doNothing().when(adminService).eliminarTopic("foo");

        mockMvc.perform(delete("/api/kafka/topics/foo")
                .with(jwt().authorities(ROLE_ADMIN, SCOPE_DELETE)))
            .andExpect(status().isOk());
    }
}

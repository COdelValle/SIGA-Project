package cl.siga.msrabbitmqadmin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.siga.coreshare.exception.GlobalExceptionHandler;
import cl.siga.coreshare.dto.rabbitmq.SolicitudColaDTO;
import cl.siga.msrabbitmqadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(RabbitAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RabbitAdminControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitAdminService service;

    @Test
    void rechazaNombreDeColaVacioAntesDeInvocarAlBroker() throws Exception {
        mockMvc.perform(delete("/api/v1/rabbitmq/queues/{nombre}", " "))
            .andExpect(status().isBadRequest());

        verify(service, never()).eliminarCola(anyString());
    }

    @Test
    void rechazaParametrosDeBindingInvalidosEnQuery() throws Exception {
        mockMvc.perform(delete("/api/v1/rabbitmq/bindings")
                .param("cola", " ")
                .param("exchange", "siga.exchange")
                .param("routingKey", "ruta con espacios"))
            .andExpect(status().isBadRequest());

        verify(service, never()).eliminarBinding(any());
    }

    @Test
    void rechazaConfiguracionDeadLetterIncompletaAntesDeCrearLaCola() throws Exception {
        mockMvc.perform(post("/api/v1/rabbitmq/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"cola.prueba","deadLetterExchange":"siga.dlx.direct","deadLetterRoutingKey":null}
                    """))
            .andExpect(status().isBadRequest());

        verify(service, never()).declararCola(any(SolicitudColaDTO.class));
    }
}

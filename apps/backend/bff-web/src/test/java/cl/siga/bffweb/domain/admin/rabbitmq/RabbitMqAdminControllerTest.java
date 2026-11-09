package cl.siga.bffweb.domain.admin.rabbitmq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import cl.siga.coreshare.exception.GlobalExceptionHandler;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RabbitMqAdminController.class)
@AutoConfigureMockMvc(addFilters = false) // Ignora Security para probar solo la validación web
@Import(GlobalExceptionHandler.class)
class RabbitMqAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitMqAdminBffService service;

    // --- Criterio: 400 Bad Request para nombres vacíos o caracteres inválidos en PathVariable ---
    @ParameterizedTest
    @ValueSource(strings = {" ", "cola invalida", "cola!1", "cola:2", "cola@test"})
    void estadoCola_ConNombreInvalido_Retorna400YNoInvocaServicio(String nombreInvalido) throws Exception {
        mockMvc.perform(get("/api/bff/v1/admin/rabbitmq/queues/{nombre}", nombreInvalido))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // --- Criterio: 400 Bad Request en RequestParam (Bindings) ---
    @ParameterizedTest
    @ValueSource(strings = {" ", "exch/invalido", "exch:test"})
    void eliminarBinding_ConExchangeInvalido_Retorna400YNoInvocaServicio(String exchangeInvalido) throws Exception {
        mockMvc.perform(delete("/api/bff/v1/admin/rabbitmq/bindings")
                .param("cola", "cola-valida")
                .param("exchange", exchangeInvalido))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // --- Criterio: Las solicitudes inválidas en Body no invocan al servicio ---
    @Test
    void declararCola_ConBodyInvalido_Retorna400YNoInvocaServicio() throws Exception {
        String jsonInvalido = "{\"nombre\": \"cola/invalida\", \"durable\": true}";
        
        mockMvc.perform(post("/api/bff/v1/admin/rabbitmq/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // --- Criterio: Las solicitudes válidas continúan llegando al servicio ---
    @Test
    void eliminarCola_ConNombreValido_Retorna204YInvocaServicio() throws Exception {
        String nombreValido = "cola.notificaciones-v1";

        mockMvc.perform(delete("/api/bff/v1/admin/rabbitmq/queues/{nombre}", nombreValido))
                .andExpect(status().isNoContent());

        verify(service).eliminarCola(nombreValido);
    }
}
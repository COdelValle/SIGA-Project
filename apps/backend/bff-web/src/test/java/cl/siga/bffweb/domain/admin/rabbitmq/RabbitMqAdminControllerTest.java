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

import cl.siga.coreshare.dto.rabbitmq.EstadoColaDTO;
import cl.siga.coreshare.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @Test
    void declararCola_ConBodyValido_Retorna201EInvocaServicio() throws Exception {
        String json = "{\"nombre\": \"cola.notificaciones-v1\", \"durable\": true}";

        mockMvc.perform(post("/api/bff/v1/admin/rabbitmq/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        verify(service).declararCola(any());
    }

    @Test
    void estadoCola_ConNombreValido_RetornaEstadoDelServicio() throws Exception {
        when(service.estadoCola("cola.notificaciones-v1"))
                .thenReturn(new EstadoColaDTO("cola.notificaciones-v1", 5, 2));

        mockMvc.perform(get("/api/bff/v1/admin/rabbitmq/queues/{nombre}", "cola.notificaciones-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("cola.notificaciones-v1"))
                .andExpect(jsonPath("$.mensajes").value(5));

        verify(service).estadoCola("cola.notificaciones-v1");
    }

    @Test
    void declararExchange_ConBodyValido_Retorna201EInvocaServicio() throws Exception {
        String json = "{\"nombre\": \"exchange.notificaciones\", \"tipo\": \"DIRECT\"}";

        mockMvc.perform(post("/api/bff/v1/admin/rabbitmq/exchanges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        verify(service).declararExchange(any());
    }

    @Test
    void eliminarExchange_ConNombreValido_Retorna204EInvocaServicio() throws Exception {
        mockMvc.perform(delete("/api/bff/v1/admin/rabbitmq/exchanges/{nombre}", "exchange.notificaciones"))
                .andExpect(status().isNoContent());

        verify(service).eliminarExchange("exchange.notificaciones");
    }

    @Test
    void declararBinding_ConBodyValido_Retorna201EInvocaServicio() throws Exception {
        String json = "{\"cola\": \"cola.notificaciones-v1\", \"exchange\": \"exchange.notificaciones\", \"routingKey\": \"notificacion\"}";

        mockMvc.perform(post("/api/bff/v1/admin/rabbitmq/bindings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        verify(service).declararBinding(any());
    }

    @Test
    void eliminarBinding_ConParametrosValidos_Retorna204EInvocaServicio() throws Exception {
        mockMvc.perform(delete("/api/bff/v1/admin/rabbitmq/bindings")
                .param("cola", "cola.notificaciones-v1")
                .param("exchange", "exchange.notificaciones")
                .param("routingKey", "notificacion"))
                .andExpect(status().isNoContent());

        verify(service).eliminarBinding("cola.notificaciones-v1", "exchange.notificaciones", "notificacion");
    }
}
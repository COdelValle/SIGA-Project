package cl.siga.msnotas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msnotas.service.NotaService;

@WebMvcTest(controllers = NotaController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class NotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotaService notaService;

    private NotaResponseDTO respuesta() {
        return new NotaResponseDTO(1L, 1L, 9L, 6.5);
    }

    @Test
    void getNotaByIdRetornaOk() throws Exception {
        when(notaService.getNotaById(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/notas/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.score").value(6.5));

        verify(notaService).getNotaById(1L);
    }

    @Test
    void searchNotasRetornaPagina() throws Exception {
        when(notaService.searchNotas(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/v1/notas/search")
                .param("idEstudiante", "1")
                .param("idEvaluacion", "9")
                .param("lessThanScore", "7.0")
                .param("greaterThanScore", "1.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1));

        verify(notaService).searchNotas(
            eq(1L), eq(9L), eq(7.0), eq(1.0), any(Pageable.class));
    }

    @Test
    void registrarNotaRetornaCreated() throws Exception {
        RegistrarNotaRequestDTO request = new RegistrarNotaRequestDTO(1L, 9L, 6.5);
        when(notaService.saveNota(any(RegistrarNotaRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/notas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(notaService).saveNota(any(RegistrarNotaRequestDTO.class));
    }

    @Test
    void updateNotaRetornaOk() throws Exception {
        ActualizarNotaRequestDTO request = new ActualizarNotaRequestDTO(6.5);
        when(notaService.updateNota(eq(1L), any(ActualizarNotaRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/notas/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").value(6.5));

        verify(notaService).updateNota(eq(1L), any(ActualizarNotaRequestDTO.class));
    }

    @Test
    void deleteNotaRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/notas/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(notaService).deleteNota(1L);
    }
}

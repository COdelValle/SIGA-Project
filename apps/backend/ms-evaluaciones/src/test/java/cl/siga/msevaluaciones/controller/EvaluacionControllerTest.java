package cl.siga.msevaluaciones.controller;

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

import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msevaluaciones.service.EvaluacionService;

@WebMvcTest(controllers = EvaluacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class EvaluacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EvaluacionService evaluacionService;

    private EvaluacionResponseDTO respuesta() {
        return new EvaluacionResponseDTO(1L, "PRUEBA", TipoEvaluacion.SUMATIVA, 30.0, 5L, true);
    }

    @Test
    void getEvaluacionByIdRetornaOk() throws Exception {
        when(evaluacionService.getEvaluacionById(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/evaluaciones/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombre").value("PRUEBA"));

        verify(evaluacionService).getEvaluacionById(1L);
    }

    @Test
    void searchEvaluacionesRetornaPagina() throws Exception {
        when(evaluacionService.searchEvaluaciones(any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/v1/evaluaciones/search")
                .param("nombre", "PRUEBA")
                .param("tipo", "SUMATIVA")
                .param("idCursoAsignatura", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1));

        verify(evaluacionService).searchEvaluaciones(
            eq("PRUEBA"), eq(TipoEvaluacion.SUMATIVA), eq(5L), any(Pageable.class));
    }

    @Test
    void registrarEvaluacionRetornaCreated() throws Exception {
        RegistrarEvaluacionRequestDTO request =
            new RegistrarEvaluacionRequestDTO("PRUEBA", TipoEvaluacion.SUMATIVA, 30.0, 5L);
        when(evaluacionService.saveEvaluacion(any(RegistrarEvaluacionRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/evaluaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(evaluacionService).saveEvaluacion(any(RegistrarEvaluacionRequestDTO.class));
    }

    @Test
    void updateEvaluacionRetornaOk() throws Exception {
        ActualizarEvaluacionRequestDTO request =
            new ActualizarEvaluacionRequestDTO("PRUEBA", TipoEvaluacion.SUMATIVA, 30.0);
        when(evaluacionService.updateEvaluacion(eq(1L), any(ActualizarEvaluacionRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/evaluaciones/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(evaluacionService).updateEvaluacion(eq(1L), any(ActualizarEvaluacionRequestDTO.class));
    }

    @Test
    void deleteEvaluacionRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/evaluaciones/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(evaluacionService).deleteEvaluacion(1L);
    }

    @Test
    void existsEvaluacionByIdRetornaBooleano() throws Exception {
        when(evaluacionService.existsEvaluacionById(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/evaluaciones/exists/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));

        verify(evaluacionService).existsEvaluacionById(1L);
    }
}

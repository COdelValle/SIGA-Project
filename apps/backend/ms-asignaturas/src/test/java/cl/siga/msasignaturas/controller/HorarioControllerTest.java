package cl.siga.msasignaturas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msasignaturas.service.HorarioService;

@WebMvcTest(controllers = HorarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class HorarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HorarioService horarioService;

    private HorarioRequestDTO request() {
        return new HorarioRequestDTO(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30), "Sala 101");
    }

    private HorarioResponseDTO respuesta() {
        return new HorarioResponseDTO(
            1L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30), "Sala 101");
    }

    @Test
    void registrarHorarioRetornaCreated() throws Exception {
        when(horarioService.crearHorario(eq(2L), any(HorarioRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/horarios/curso-asignatura/{cursoAsignaturaId}", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(horarioService).crearHorario(eq(2L), any(HorarioRequestDTO.class));
    }

    @Test
    void updateHorarioRetornaOk() throws Exception {
        when(horarioService.actualizarHorario(eq(1L), any(HorarioRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/horarios/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(horarioService).actualizarHorario(eq(1L), any(HorarioRequestDTO.class));
    }

    @Test
    void deleteHorarioRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/horarios/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(horarioService).eliminarHorario(1L);
    }
}

package cl.siga.msasistencias.controller;

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

import java.time.LocalDate;
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

import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msasistencias.service.AsistenciaService;

@WebMvcTest(controllers = AsistenciaController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class AsistenciaControllerTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AsistenciaService asistenciaService;

    private AsistenciaResponseDTO respuesta() {
        return new AsistenciaResponseDTO(2L, 1L, 5L, FECHA, State.AUSENTE,
            Justificacion.PENDIENTE, "sin aviso");
    }

    @Test
    void getAsistenciaByIdRetornaOk() throws Exception {
        when(asistenciaService.getAsistenciaById(2L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/asistencias/{id}", 2L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.estado").value("AUSENTE"));

        verify(asistenciaService).getAsistenciaById(2L);
    }

    @Test
    void searchAsistenciasRetornaPagina() throws Exception {
        when(asistenciaService.searchAsistencias(any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/v1/asistencias/search")
                .param("idEstudiante", "1")
                .param("idCursoAsignatura", "5")
                .param("from", "2026-10-01")
                .param("to", "2026-10-31")
                .param("estado", "AUSENTE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(2));

        verify(asistenciaService).searchAsistencias(
            eq(1L), eq(5L), eq(FECHA), eq(LocalDate.of(2026, 10, 31)),
            eq(State.AUSENTE), any(Pageable.class));
    }

    @Test
    void registrarAsistenciaRetornaCreated() throws Exception {
        RegistrarAsistenciaRequestDTO request =
            new RegistrarAsistenciaRequestDTO(1L, 5L, FECHA, State.AUSENTE, "sin aviso");
        when(asistenciaService.saveAsistencia(any(RegistrarAsistenciaRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/asistencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(2));

        verify(asistenciaService).saveAsistencia(any(RegistrarAsistenciaRequestDTO.class));
    }

    @Test
    void actualizarAsistenciaRetornaOk() throws Exception {
        ActualizarAsistenciaRequestDTO request =
            new ActualizarAsistenciaRequestDTO(Justificacion.SI, "certificado", State.AUSENTE);
        when(asistenciaService.updateAsistencia(eq(2L), any(ActualizarAsistenciaRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/asistencias/{id}", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2));

        verify(asistenciaService).updateAsistencia(eq(2L), any(ActualizarAsistenciaRequestDTO.class));
    }

    @Test
    void deleteAsistenciaRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/asistencias/{id}", 2L))
            .andExpect(status().isNoContent());

        verify(asistenciaService).deleteAsistencia(2L);
    }

    @Test
    void existsAsistenciaByIdRetornaBooleano() throws Exception {
        when(asistenciaService.existsAsistenciaById(2L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/asistencias/exists/{id}", 2L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));

        verify(asistenciaService).existsAsistenciaById(2L);
    }
}

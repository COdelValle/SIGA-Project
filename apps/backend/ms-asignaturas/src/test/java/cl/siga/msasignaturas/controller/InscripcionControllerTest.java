package cl.siga.msasignaturas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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

import cl.siga.coreshare.dto.asignatura.inscripcion.ActualizarEstadoInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.RegistrarInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msasignaturas.service.InscripcionService;

@WebMvcTest(controllers = InscripcionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class InscripcionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InscripcionService inscripcionService;

    private InscripcionResponseDTO respuesta() {
        return new InscripcionResponseDTO(
            1L, 10L, 20L, EstadoInscripcion.PRE_INSCRITO, LocalDateTime.of(2026, 3, 1, 9, 30));
    }

    @Test
    void getInscripcionByIdRetornaOk() throws Exception {
        when(inscripcionService.obtenerPorId(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/inscripciones/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.idAlumno").value(10));

        verify(inscripcionService).obtenerPorId(1L);
    }

    @Test
    void searchInscripcionesRetornaPagina() throws Exception {
        when(inscripcionService.buscarInscripciones(any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/v1/inscripciones/search")
                .param("idAlumno", "10")
                .param("idCursoAsignatura", "20")
                .param("estados", "ACTIVO", "PRE_INSCRITO"))
            .andExpect(status().isOk());

        verify(inscripcionService).buscarInscripciones(
            eq(10L), eq(20L),
            eq(List.of(EstadoInscripcion.ACTIVO, EstadoInscripcion.PRE_INSCRITO)),
            any(Pageable.class));
    }

    @Test
    void registrarInscripcionRetornaCreated() throws Exception {
        RegistrarInscripcionRequestDTO request = new RegistrarInscripcionRequestDTO(10L, 20L);
        when(inscripcionService.registrarInscripcion(any(RegistrarInscripcionRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/inscripciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(inscripcionService).registrarInscripcion(any(RegistrarInscripcionRequestDTO.class));
    }

    @Test
    void actualizarEstadoRetornaOk() throws Exception {
        ActualizarEstadoInscripcionRequestDTO request =
            new ActualizarEstadoInscripcionRequestDTO(EstadoInscripcion.ACTIVO);
        when(inscripcionService.actualizarEstado(eq(1L), any(ActualizarEstadoInscripcionRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/inscripciones/{id}/estado", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(inscripcionService).actualizarEstado(eq(1L), any(ActualizarEstadoInscripcionRequestDTO.class));
    }
}

package cl.siga.msestudiantes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.enums.State;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msestudiantes.service.EstudianteService;

@WebMvcTest(controllers = EstudianteController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class EstudianteControllerTest {

    private static final String ID_USUARIO = "123e4567-e89b-12d3-a456-426614174000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EstudianteService estudianteService;

    private EstudianteResponseDTO respuesta() {
        return new EstudianteResponseDTO(
            1L,
            ID_USUARIO,
            "11111111-1",
            "Ana",
            "María",
            "Pérez",
            "González",
            LocalDate.of(2010, 5, 10),
            List.of("Polen"),
            State.REGULAR.name(),
            3L
        );
    }

    @Test
    void getEstudianteByIdRetornaOk() throws Exception {
        when(estudianteService.getEstudianteById(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/estudiantes/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.rut").value("11111111-1"));

        verify(estudianteService).getEstudianteById(1L);
    }

    @Test
    void getEstudianteByIdUsuarioRetornaOk() throws Exception {
        when(estudianteService.getEstudianteByIdUsuario(ID_USUARIO)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/estudiantes/idUsuario/{idUsuario}", ID_USUARIO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idUsuario").value(ID_USUARIO));

        verify(estudianteService).getEstudianteByIdUsuario(ID_USUARIO);
    }

    @Test
    void searchEstudiantesRetornaPagina() throws Exception {
        when(estudianteService.searchEstudiantes(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/v1/estudiantes/search")
                .param("state", "REGULAR")
                .param("idClase", "3"))
            .andExpect(status().isOk());

        verify(estudianteService).searchEstudiantes(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(State.REGULAR), eq(3L), any(Pageable.class));
    }

    @Test
    void registrarEstudianteRetornaCreated() throws Exception {
        RegistrarEstudianteRequestDTO request = new RegistrarEstudianteRequestDTO(
            ID_USUARIO,
            "Ana",
            "María",
            "Pérez",
            "González",
            "11111111-1",
            LocalDate.of(2010, 5, 10),
            List.of("Polen"),
            3L
        );
        when(estudianteService.saveEstudiante(any(RegistrarEstudianteRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/estudiantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(estudianteService).saveEstudiante(any(RegistrarEstudianteRequestDTO.class));
    }

    @Test
    void actualizarEstudianteRetornaOk() throws Exception {
        ActualizarEstudianteRequestDTO request = new ActualizarEstudianteRequestDTO(
            "Ana",
            "María",
            "Pérez",
            "González",
            LocalDate.of(2010, 5, 10),
            List.of("Polen"),
            3L
        );
        when(estudianteService.updateEstudiante(eq(1L), any(ActualizarEstudianteRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/estudiantes/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(estudianteService).updateEstudiante(eq(1L), any(ActualizarEstudianteRequestDTO.class));
    }

    @Test
    void existsEstudianteByIdRetornaBooleano() throws Exception {
        when(estudianteService.existsEstudianteById(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/estudiantes/exists/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));

        verify(estudianteService).existsEstudianteById(1L);
    }

    @Test
    void deleteEstudianteRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/estudiantes/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(estudianteService).deleteEstudiante(1L);
    }
}

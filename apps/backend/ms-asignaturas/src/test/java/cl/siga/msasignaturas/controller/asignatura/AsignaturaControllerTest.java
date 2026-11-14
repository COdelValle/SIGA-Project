package cl.siga.msasignaturas.controller.asignatura;

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

import cl.siga.coreshare.dto.asignatura.ActualizarAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.RegistrarAsignaturaRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msasignaturas.service.asignatura.AsignaturaService;

@WebMvcTest(controllers = AsignaturaController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class AsignaturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AsignaturaService asignaturaService;

    private AsignaturaResponseDTO respuesta() {
        return new AsignaturaResponseDTO(
            1L, "Matemática", "MAT", "Asignatura de matemática", AreaAcademica.MATEMATICAS, true, true);
    }

    @Test
    void getAsignaturaByIdRetornaOk() throws Exception {
        when(asignaturaService.obtenerPorId(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/asignaturas/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombre").value("Matemática"));

        verify(asignaturaService).obtenerPorId(1L);
    }

    @Test
    void buscarAsignaturasRetornaPagina() throws Exception {
        when(asignaturaService.searchAsignaturas(any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/v1/asignaturas/search")
                .param("nombre", "Matemática")
                .param("area", "MATEMATICAS")
                .param("calificable", "true"))
            .andExpect(status().isOk());

        verify(asignaturaService).searchAsignaturas(
            eq("Matemática"), eq(AreaAcademica.MATEMATICAS), eq(true), any(Pageable.class));
    }

    @Test
    void registrarAsignaturaRetornaCreated() throws Exception {
        RegistrarAsignaturaRequestDTO request = new RegistrarAsignaturaRequestDTO(
            "Matemática", "MAT", "Asignatura de matemática", AreaAcademica.MATEMATICAS, true);
        when(asignaturaService.crear(any(RegistrarAsignaturaRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(asignaturaService).crear(any(RegistrarAsignaturaRequestDTO.class));
    }

    @Test
    void actualizarAsignaturaRetornaOk() throws Exception {
        ActualizarAsignaturaRequestDTO request = new ActualizarAsignaturaRequestDTO(
            "Matemática", "MAT", "Asignatura de matemática", AreaAcademica.MATEMATICAS, true);
        when(asignaturaService.actualizar(eq(1L), any(ActualizarAsignaturaRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/asignaturas/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(asignaturaService).actualizar(eq(1L), any(ActualizarAsignaturaRequestDTO.class));
    }

    @Test
    void deleteAsignaturaRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/asignaturas/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(asignaturaService).eliminar(1L);
    }

    @Test
    void existsAsignaturaByIdRetornaBooleano() throws Exception {
        when(asignaturaService.existAsignatura(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/asignaturas/exists/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));

        verify(asignaturaService).existAsignatura(1L);
    }
}

package cl.siga.msclases.controller;

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

import cl.siga.coreshare.dto.clase.ActualizarDocenteJefeRequestDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.RegistrarClaseRequestDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msclases.service.ClaseService;

@WebMvcTest(controllers = ClaseController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class ClaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClaseService claseService;

    private ClaseResponseDTO respuesta() {
        return new ClaseResponseDTO(1L, Nivel.PRIMERO_BASICO, "A", 2026, 5L);
    }

    @Test
    void getClaseByIdRetornaOk() throws Exception {
        when(claseService.getClaseById(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/clases/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.letra").value("A"));

        verify(claseService).getClaseById(1L);
    }

    @Test
    void searchClasesRetornaPagina() throws Exception {
        when(claseService.searchClases(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/v1/clases/search")
                .param("nivel", "PRIMERO_BASICO")
                .param("letra", "A")
                .param("anioAcademico", "2026")
                .param("idDocenteJefe", "5"))
            .andExpect(status().isOk());

        verify(claseService).searchClases(
            eq(Nivel.PRIMERO_BASICO), eq("A"), eq(2026), eq(5L), any(Pageable.class));
    }

    @Test
    void registrarClaseRetornaCreated() throws Exception {
        RegistrarClaseRequestDTO request = new RegistrarClaseRequestDTO(
            Nivel.PRIMERO_BASICO, "A", 2026, 5L);
        when(claseService.saveClase(any(RegistrarClaseRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/clases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(claseService).saveClase(any(RegistrarClaseRequestDTO.class));
    }

    @Test
    void actualizarDocenteJefeRetornaOk() throws Exception {
        ActualizarDocenteJefeRequestDTO request = new ActualizarDocenteJefeRequestDTO(5L);
        when(claseService.updateDocenteJefe(eq(1L), any(ActualizarDocenteJefeRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/clases/{id}/docente-jefe", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(claseService).updateDocenteJefe(eq(1L), any(ActualizarDocenteJefeRequestDTO.class));
    }

    @Test
    void existsClaseByIdRetornaBooleano() throws Exception {
        when(claseService.existsClaseById(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/clases/exists/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));

        verify(claseService).existsClaseById(1L);
    }

    @Test
    void deleteClaseRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/clases/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(claseService).deleteClase(1L);
    }
}

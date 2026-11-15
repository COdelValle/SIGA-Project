package cl.siga.msapoderados.controller;

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

import cl.siga.coreshare.dto.apoderado.ActualizarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.RegistrarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msapoderados.service.ApoderadoService;

@WebMvcTest(controllers = ApoderadoController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class ApoderadoControllerTest {

    private static final String ID_USUARIO = "123e4567-e89b-12d3-a456-426614174001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApoderadoService apoderadoService;

    private ApoderadoResponseDTO respuesta() {
        return new ApoderadoResponseDTO(
            1L,
            ID_USUARIO,
            "Carla",
            "Andrea",
            "Muñoz",
            "Silva",
            "11111111-1",
            List.of("+56912345678"),
            List.of(new ParentescoEstudianteDTO(10L, Parentesco.MADRE_PADRE)),
            true
        );
    }

    @Test
    void getApoderadoByIdRetornaOk() throws Exception {
        when(apoderadoService.getApoderadoById(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/apoderados/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.rut").value("11111111-1"));

        verify(apoderadoService).getApoderadoById(1L);
    }

    @Test
    void getApoderadoByIdUsuarioRetornaOk() throws Exception {
        when(apoderadoService.getApoderadoByIdUsuario(ID_USUARIO)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/apoderados/idUsuario/{idUsuario}", ID_USUARIO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idUsuario").value(ID_USUARIO));

        verify(apoderadoService).getApoderadoByIdUsuario(ID_USUARIO);
    }

    @Test
    void searchApoderadosRetornaPagina() throws Exception {
        when(apoderadoService.searchApoderados(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/v1/apoderados/search")
                .param("rut", "11111111-1")
                .param("idEstudiante", "10"))
            .andExpect(status().isOk());

        verify(apoderadoService).searchApoderados(
            eq("11111111-1"), isNull(), isNull(), eq(10L), any(Pageable.class));
    }

    @Test
    void registrarApoderadoRetornaCreated() throws Exception {
        RegistrarApoderadoRequestDTO request = new RegistrarApoderadoRequestDTO(
            ID_USUARIO,
            "Carla",
            "Andrea",
            "Muñoz",
            "Silva",
            "11111111-1",
            List.of("+56912345678"),
            List.of(new ParentescoEstudianteDTO(10L, Parentesco.MADRE_PADRE))
        );
        when(apoderadoService.saveApoderado(any(RegistrarApoderadoRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/apoderados")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(apoderadoService).saveApoderado(any(RegistrarApoderadoRequestDTO.class));
    }

    @Test
    void actualizarApoderadoRetornaOk() throws Exception {
        ActualizarApoderadoRequestDTO request = new ActualizarApoderadoRequestDTO(
            "Carla",
            "Andrea",
            "Muñoz",
            "Silva",
            List.of("+56912345678")
        );
        when(apoderadoService.updateApoderado(eq(1L), any(ActualizarApoderadoRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/apoderados/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(apoderadoService).updateApoderado(eq(1L), any(ActualizarApoderadoRequestDTO.class));
    }

    @Test
    void addEstudianteRetornaOk() throws Exception {
        ParentescoEstudianteDTO request = new ParentescoEstudianteDTO(10L, Parentesco.TUTOR_LEGAL);
        when(apoderadoService.addEstudiante(eq(1L), any(ParentescoEstudianteDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/apoderados/{id}/estudiantes", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(apoderadoService).addEstudiante(eq(1L), any(ParentescoEstudianteDTO.class));
    }

    @Test
    void removeEstudianteRetornaOk() throws Exception {
        when(apoderadoService.removeEstudiante(1L, 10L)).thenReturn(respuesta());

        mockMvc.perform(delete("/api/v1/apoderados/{id}/estudiantes/{idEstudiante}", 1L, 10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(apoderadoService).removeEstudiante(1L, 10L);
    }

    @Test
    void existsApoderadoByIdRetornaBooleano() throws Exception {
        when(apoderadoService.existsApoderadoById(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/apoderados/exists/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));

        verify(apoderadoService).existsApoderadoById(1L);
    }

    @Test
    void deleteApoderadoRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/apoderados/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(apoderadoService).deleteApoderado(1L);
    }
}

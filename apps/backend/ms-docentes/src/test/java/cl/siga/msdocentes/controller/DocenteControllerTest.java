package cl.siga.msdocentes.controller;

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

import cl.siga.coreshare.dto.docente.ActualizarDocenteRequestDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.docente.RegistrarDocenteRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoResponseDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msdocentes.service.DocenteService;

@WebMvcTest(controllers = DocenteController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class DocenteControllerTest {

    private static final String ID_USUARIO = "123e4567-e89b-12d3-a456-426614174002";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocenteService docenteService;

    private DocenteResponseDTO respuesta() {
        return new DocenteResponseDTO(
            1L,
            ID_USUARIO,
            "Pedro",
            "José",
            "Ramírez",
            "Soto",
            "11111111-1",
            LocalDate.of(2015, 3, 1),
            true,
            AreaAcademica.MATEMATICAS,
            List.of(new CertificadoResponseDTO(
                5L, "Magíster en Educación", "Universidad de Chile", LocalDate.of(2014, 1, 15)))
        );
    }

    @Test
    void getDocenteByIdRetornaOk() throws Exception {
        when(docenteService.getDocenteById(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/docentes/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.rut").value("11111111-1"));

        verify(docenteService).getDocenteById(1L);
    }

    @Test
    void getDocenteByIdUsuarioRetornaOk() throws Exception {
        when(docenteService.getDocenteByIdUsuario(ID_USUARIO)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/docentes/idUsuario/{idUsuario}", ID_USUARIO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idUsuario").value(ID_USUARIO));

        verify(docenteService).getDocenteByIdUsuario(ID_USUARIO);
    }

    @Test
    void searchDocentesRetornaPagina() throws Exception {
        when(docenteService.searchDocentes(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/v1/docentes/search")
                .param("area", "MATEMATICAS")
                .param("rut", "11111111-1"))
            .andExpect(status().isOk());

        verify(docenteService).searchDocentes(
            eq("11111111-1"), isNull(), isNull(), isNull(), isNull(),
            eq(AreaAcademica.MATEMATICAS), any(Pageable.class));
    }

    @Test
    void registrarDocenteRetornaCreated() throws Exception {
        RegistrarDocenteRequestDTO request = new RegistrarDocenteRequestDTO(
            ID_USUARIO,
            "Pedro",
            "José",
            "Ramírez",
            "Soto",
            "11111111-1",
            LocalDate.of(2015, 3, 1),
            AreaAcademica.MATEMATICAS,
            List.of(new CertificadoRequestDTO(
                "Magíster en Educación", "Universidad de Chile", LocalDate.of(2014, 1, 15)))
        );
        when(docenteService.saveDocente(any(RegistrarDocenteRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/docentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(docenteService).saveDocente(any(RegistrarDocenteRequestDTO.class));
    }

    @Test
    void actualizarDocenteRetornaOk() throws Exception {
        ActualizarDocenteRequestDTO request = new ActualizarDocenteRequestDTO(
            "Pedro",
            "José",
            "Ramírez",
            "Soto",
            LocalDate.of(2015, 3, 1),
            AreaAcademica.CIENCIAS
        );
        when(docenteService.updateDocente(eq(1L), any(ActualizarDocenteRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/docentes/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(docenteService).updateDocente(eq(1L), any(ActualizarDocenteRequestDTO.class));
    }

    @Test
    void existsDocenteByIdRetornaBooleano() throws Exception {
        when(docenteService.existsDocenteById(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/docentes/exists/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));

        verify(docenteService).existsDocenteById(1L);
    }

    @Test
    void deleteDocenteRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/docentes/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(docenteService).deleteDocente(1L);
    }
}

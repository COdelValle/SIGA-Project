package cl.siga.msdocentes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoResponseDTO;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msdocentes.service.CertificadoService;

@WebMvcTest(controllers = CertificadoController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class CertificadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CertificadoService certificadoService;

    private CertificadoResponseDTO respuesta() {
        return new CertificadoResponseDTO(
            5L, "Magíster en Educación", "Universidad de Chile", LocalDate.of(2014, 1, 15));
    }

    @Test
    void getCertificadosRetornaLista() throws Exception {
        when(certificadoService.getCertificadosByDocenteId(1L)).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/v1/docentes/{docenteId}/certificados", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(5));

        verify(certificadoService).getCertificadosByDocenteId(1L);
    }

    @Test
    void addCertificadoRetornaCreated() throws Exception {
        CertificadoRequestDTO request = new CertificadoRequestDTO(
            "Magíster en Educación", "Universidad de Chile", LocalDate.of(2014, 1, 15));
        when(certificadoService.addCertificado(eq(1L), any(CertificadoRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/docentes/{docenteId}/certificados", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(5));

        verify(certificadoService).addCertificado(eq(1L), any(CertificadoRequestDTO.class));
    }

    @Test
    void deleteCertificadoRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/docentes/{docenteId}/certificados/{certificadoId}", 1L, 5L))
            .andExpect(status().isNoContent());

        verify(certificadoService).deleteCertificado(1L, 5L);
    }
}

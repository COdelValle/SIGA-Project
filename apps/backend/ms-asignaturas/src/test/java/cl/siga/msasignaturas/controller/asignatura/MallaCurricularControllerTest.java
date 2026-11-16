package cl.siga.msasignaturas.controller.asignatura;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.asignatura.malla.ActualizarMallaCurricularRequestDTO;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.asignatura.malla.RegistrarMallaCurricularRequestDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msasignaturas.service.asignatura.MallaCurricularService;

@WebMvcTest(controllers = MallaCurricularController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class MallaCurricularControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MallaCurricularService mallaCurricularService;

    private MallaCurricularResponseDTO respuesta() {
        return new MallaCurricularResponseDTO(
            1L,
            Nivel.PRIMERO_BASICO,
            2L,
            "Matemática",
            AreaAcademica.MATEMATICAS,
            CaracterAsignatura.OBLIGATORIA,
            PlanFormacion.COMUN,
            6,
            true,
            true
        );
    }

    @Test
    void listarMallaRetornaLista() throws Exception {
        when(mallaCurricularService.listar(eq(Nivel.PRIMERO_BASICO), eq(PlanFormacion.COMUN)))
            .thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/v1/malla")
                .param("nivel", "PRIMERO_BASICO")
                .param("plan", "COMUN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));

        verify(mallaCurricularService).listar(Nivel.PRIMERO_BASICO, PlanFormacion.COMUN);
    }

    @Test
    void registrarMallaRetornaCreated() throws Exception {
        RegistrarMallaCurricularRequestDTO request = new RegistrarMallaCurricularRequestDTO(
            Nivel.PRIMERO_BASICO, 2L, CaracterAsignatura.OBLIGATORIA, PlanFormacion.COMUN, 6);
        when(mallaCurricularService.crear(any(RegistrarMallaCurricularRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/malla")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(mallaCurricularService).crear(any(RegistrarMallaCurricularRequestDTO.class));
    }

    @Test
    void actualizarMallaRetornaOk() throws Exception {
        ActualizarMallaCurricularRequestDTO request = new ActualizarMallaCurricularRequestDTO(
            CaracterAsignatura.OPTATIVA, PlanFormacion.COMUN, 4);
        when(mallaCurricularService.actualizar(eq(1L), any(ActualizarMallaCurricularRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/malla/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(mallaCurricularService).actualizar(eq(1L), any(ActualizarMallaCurricularRequestDTO.class));
    }

    @Test
    void deleteMallaRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/malla/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(mallaCurricularService).eliminar(1L);
    }
}

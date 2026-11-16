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

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

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

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.curso.ActualizarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.curso.RegistrarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.security.JwtResourceServerConfiguration;
import cl.siga.coreshare.security.SharedSecurityConfig;
import cl.siga.msasignaturas.service.asignatura.CursoAsignaturaService;

@WebMvcTest(controllers = CursoAsignaturaController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = { SharedSecurityConfig.class, JwtResourceServerConfiguration.class })
class CursoAsignaturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CursoAsignaturaService cursoAsignaturaService;

    private CursoAsignaturaResponseDTO respuesta() {
        return new CursoAsignaturaResponseDTO(
            1L,
            2L,
            "Matemática",
            "Asignatura de matemática",
            AreaAcademica.MATEMATICAS,
            true,
            CaracterAsignatura.OPTATIVA,
            Semestre.SEMESTRE_1,
            3L,
            4L,
            30,
            25,
            5,
            List.of(new HorarioResponseDTO(
                6L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30), "Sala 101"))
        );
    }

    private HorarioRequestDTO horarioRequest() {
        return new HorarioRequestDTO(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30), "Sala 101");
    }

    @Test
    void getCursoAsignaturaByIdRetornaOk() throws Exception {
        when(cursoAsignaturaService.obtenerPorId(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/curso-asignaturas/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.idAsignatura").value(2));

        verify(cursoAsignaturaService).obtenerPorId(1L);
    }

    @Test
    void buscarCursoAsignaturasRetornaPagina() throws Exception {
        when(cursoAsignaturaService.searchCursoAsignaturas(
            any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(respuesta())));

        mockMvc.perform(get("/api/v1/curso-asignaturas/search")
                .param("idClase", "4")
                .param("idDocente", "3")
                .param("caracter", "OPTATIVA")
                .param("semestre", "SEMESTRE_1")
                .param("area", "MATEMATICAS")
                .param("conCupoDisponible", "true"))
            .andExpect(status().isOk());

        verify(cursoAsignaturaService).searchCursoAsignaturas(
            eq(4L), eq(3L), isNull(), eq(CaracterAsignatura.OPTATIVA), eq(Semestre.SEMESTRE_1),
            eq(AreaAcademica.MATEMATICAS), isNull(), eq(true), any(Pageable.class));
    }

    @Test
    void registrarCursoAsignaturaRetornaCreated() throws Exception {
        RegistrarCursoAsignaturaRequestDTO request = new RegistrarCursoAsignaturaRequestDTO(
            2L, 4L, 3L, Semestre.SEMESTRE_1, CaracterAsignatura.OPTATIVA, 30, List.of(horarioRequest()));
        when(cursoAsignaturaService.crear(any(RegistrarCursoAsignaturaRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/curso-asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(cursoAsignaturaService).crear(any(RegistrarCursoAsignaturaRequestDTO.class));
    }

    @Test
    void actualizarCursoAsignaturaRetornaOk() throws Exception {
        ActualizarCursoAsignaturaRequestDTO request = new ActualizarCursoAsignaturaRequestDTO(
            3L, Semestre.SEMESTRE_2, CaracterAsignatura.ELECTIVA, 20);
        when(cursoAsignaturaService.actualizar(eq(1L), any(ActualizarCursoAsignaturaRequestDTO.class)))
            .thenReturn(respuesta());

        mockMvc.perform(put("/api/v1/curso-asignaturas/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(cursoAsignaturaService).actualizar(eq(1L), any(ActualizarCursoAsignaturaRequestDTO.class));
    }

    @Test
    void deleteCursoAsignaturaRetornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/curso-asignaturas/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(cursoAsignaturaService).eliminar(1L);
    }

    @Test
    void existsCursoAsignaturaByIdRetornaBooleano() throws Exception {
        when(cursoAsignaturaService.existCursoAsignatura(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/curso-asignaturas/exists/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));

        verify(cursoAsignaturaService).existCursoAsignatura(1L);
    }

    @Test
    void validarMineducRetornaMapa() throws Exception {
        when(cursoAsignaturaService.validarCumplimientoMineduc()).thenReturn(Map.of(
            "totalElectivosOfrecidos", 6,
            "areasCubiertas", 2,
            "cumpleNormativaMineduc", true));

        mockMvc.perform(get("/api/v1/curso-asignaturas/validar-mineduc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElectivosOfrecidos").value(6));

        verify(cursoAsignaturaService).validarCumplimientoMineduc();
    }
}

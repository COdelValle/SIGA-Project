package cl.siga.bffweb.domain.evaluaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import cl.siga.bffweb.domain.docentes.DocenteContextService;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.bffweb.integration.evaluaciones.EvaluacionClient;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.enums.TipoAsignatura;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.enums.AreaAcademica;

class EvaluacionBffServiceTest {

    private final EvaluacionClient evaluacionClient = mock(EvaluacionClient.class);
    private final DocenteClient docenteClient = mock(DocenteClient.class);
    private final AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
    private final DocenteContextService docenteContext =
        new DocenteContextService(docenteClient, asignaturaClient);

    private final EvaluacionBffService service =
        new EvaluacionBffService(evaluacionClient, docenteContext);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crearEvaluacionValidaAsignaturaDelDocente() {
        autenticar("oid-alejandro");
        cuandoEsDuenio();
        RegistrarEvaluacionRequestDTO request =
            new RegistrarEvaluacionRequestDTO("PRUEBA 1", TipoEvaluacion.SUMATIVA, 40.0, 3L);
        when(evaluacionClient.saveEvaluacion(request))
            .thenReturn(new EvaluacionResponseDTO(9L, "PRUEBA 1", TipoEvaluacion.SUMATIVA, 40.0, 3L, true));

        var creada = service.crearEvaluacion(request);

        assertThat(creada.id()).isEqualTo(9L);
    }

    @Test
    void crearEvaluacionRechazaAsignaturaDeOtroDocente() {
        autenticar("oid-otro");
        when(asignaturaClient.getAsignaturaById(3L)).thenReturn(ciencias());
        when(docenteClient.getDocenteByIdUsuario("oid-otro")).thenReturn(docente(2L, "oid-otro"));

        assertThatThrownBy(() -> service.crearEvaluacion(
            new RegistrarEvaluacionRequestDTO("PRUEBA 1", TipoEvaluacion.SUMATIVA, 40.0, 3L)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void editarEvaluacionValidaPorLaEvaluacionExistente() {
        autenticar("oid-alejandro");
        cuandoEsDuenio();
        when(evaluacionClient.getEvaluacionById(9L)).thenReturn(evaluacion());
        when(evaluacionClient.updateEvaluacion(eq(9L), eq(new ActualizarEvaluacionRequestDTO(
            "PRUEBA 1", TipoEvaluacion.SUMATIVA, 35.0))))
            .thenReturn(new EvaluacionResponseDTO(9L, "PRUEBA 1", TipoEvaluacion.SUMATIVA, 35.0, 3L, true));

        var editada = service.editarEvaluacion(9L,
            new ActualizarEvaluacionRequestDTO("PRUEBA 1", TipoEvaluacion.SUMATIVA, 35.0));

        assertThat(editada.ponderacion()).isEqualTo(35.0);
    }

    @Test
    void eliminarEvaluacionValidaYDelega() {
        autenticar("oid-alejandro");
        cuandoEsDuenio();
        when(evaluacionClient.getEvaluacionById(9L)).thenReturn(evaluacion());

        service.eliminarEvaluacion(9L);

        verify(evaluacionClient).deleteEvaluacion(9L);
    }

    private void cuandoEsDuenio() {
        when(asignaturaClient.getAsignaturaById(3L)).thenReturn(ciencias());
        when(docenteClient.getDocenteByIdUsuario("oid-alejandro")).thenReturn(docente(1L, "oid-alejandro"));
    }

    private static void autenticar(String oid) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("oid", oid).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_DOCENTE"))));
    }

    private static DocenteResponseDTO docente(Long id, String oid) {
        return new DocenteResponseDTO(id, oid, "ALEJANDRO", "JAVIER", "SILVA", "MORALES",
            "11111111-1", LocalDate.of(2019, 3, 1), true, AreaAcademica.CIENCIAS, List.of());
    }

    private static AsignaturaResponseDTO ciencias() {
        return new AsignaturaResponseDTO(
            3L, "CIENCIAS", "ciencias naturales", Semestre.SEMESTRE_1, AreaAcademica.CIENCIAS,
            TipoAsignatura.BASICA, 1L, List.of(), 4L, null, null, null, List.of());
    }

    private static EvaluacionResponseDTO evaluacion() {
        return new EvaluacionResponseDTO(9L, "PRUEBA 1", TipoEvaluacion.SUMATIVA, 40.0, 3L, true);
    }
}

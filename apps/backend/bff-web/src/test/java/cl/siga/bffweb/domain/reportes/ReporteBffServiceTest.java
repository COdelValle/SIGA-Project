package cl.siga.bffweb.domain.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.siga.bffweb.domain.docentes.DocenteContextService;
import cl.siga.bffweb.integration.apoderados.ApoderadoClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.bffweb.integration.reportes.ReporteClient;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.reportes.IndicadorAsistenciaDTO;
import cl.siga.coreshare.dto.reportes.IndicadorRendimientoDTO;
import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import cl.siga.coreshare.dto.asistencia.enums.State;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class ReporteBffServiceTest {

    private final ReporteClient reporteClient = mock(ReporteClient.class);
    private final EstudianteClient estudianteClient = mock(EstudianteClient.class);
    private final ApoderadoClient apoderadoClient = mock(ApoderadoClient.class);
    private final DocenteContextService docenteContext = mock(DocenteContextService.class);
    private final ReporteBffService service = new ReporteBffService(
        reporteClient, estudianteClient, apoderadoClient, docenteContext);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void estudianteAutenticadoConsultaSusIndicadoresConUltimaActualizacion() {
        autenticar("oid-estudiante", "ESTUDIANTE", "SCOPE_asistencias:read");
        when(estudianteClient.getEstudianteByIdUsuario("oid-estudiante"))
            .thenReturn(new cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO(
                41L, "oid-estudiante", "11111111-1", "Ana", null, "Pérez",
                null, null, List.of(), "ACTIVO", 8L));
        IndicadorAsistenciaDTO indicador = new IndicadorAsistenciaDTO(
            41L, 8L, LocalDate.of(2026, 11, 9), State.AUSENTE,
            45.0, 20, 9, NivelRiesgo.CRITICO,
            LocalDateTime.of(2026, 11, 9, 22, 0));
        when(reporteClient.buscarAsistencia(
            eq(41L), eq(8L), eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 30))))
            .thenReturn(List.of(indicador));

        var resultado = service.buscarAsistencia(
            null, 8L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(resultado).containsExactly(indicador);
        assertThat(resultado.get(0).actualizadoEn()).isEqualTo(LocalDateTime.of(2026, 11, 9, 22, 0));
        verify(reporteClient).buscarAsistencia(
            41L, 8L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    }

    @Test
    void apoderadoConsultaRendimientoDePupiloVinculado() {
        autenticar("oid-apoderado", "APODERADO", "SCOPE_notas:read");
        when(estudianteClient.getEstudianteById(41L))
            .thenReturn(new cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO(
                41L, "oid-estudiante", "11111111-1", "Ana", null, "Pérez",
                null, null, List.of(), "ACTIVO", 8L));
        when(apoderadoClient.getApoderadoByIdUsuario("oid-apoderado"))
            .thenReturn(new ApoderadoResponseDTO(3L, "oid-apoderado", "Luis", null,
                "Pérez", null, "22222222-2", List.of(), List.of(
                    new cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO(
                        41L, cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco.MADRE_PADRE)), true));
        IndicadorRendimientoDTO indicador = new IndicadorRendimientoDTO(
            41L, 12L, 8L, 3.5, 3.5, 2, NivelRiesgo.CRITICO,
            LocalDateTime.of(2026, 11, 9, 22, 0));
        when(reporteClient.buscarRendimiento(41L, 8L, null, null))
            .thenReturn(List.of(indicador));

        var resultado = service.buscarRendimiento(41L, 8L, null, null);

        assertThat(resultado).containsExactly(indicador);
        assertThat(resultado.get(0).actualizadoEn()).isNotNull();
    }

    private void autenticar(String oid, String rol, String scope) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("oid", oid).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
            List.of(new SimpleGrantedAuthority("ROLE_" + rol),
                new SimpleGrantedAuthority(scope))));
    }
}

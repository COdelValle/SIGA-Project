package cl.siga.bffweb.domain.asistencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

import cl.siga.bffweb.integration.apoderados.ApoderadoClient;
import cl.siga.bffweb.integration.asistencias.AsistenciaClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;

class AsistenciaBffServiceTest {

    private final AsistenciaClient asistenciaClient = mock(AsistenciaClient.class);
    private final EstudianteClient estudianteClient = mock(EstudianteClient.class);
    private final ApoderadoClient apoderadoClient = mock(ApoderadoClient.class);

    private final AsistenciaBffService service = new AsistenciaBffService(
        asistenciaClient, estudianteClient, apoderadoClient);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAsistenciasMeResuelveElOidDelToken() {
        autenticar("oid-camila", "ESTUDIANTE");
        when(estudianteClient.getEstudianteByIdUsuario("oid-camila")).thenReturn(estudiante(1L, "oid-camila"));
        when(asistenciaClient.searchAsistencias(eq(1L), anyInt())).thenReturn(pagina());

        List<AsistenciaResponseDTO> asistencias = service.getAsistenciasMe();

        assertThat(asistencias).hasSize(1);
        assertThat(asistencias.get(0).estado()).isEqualTo(State.AUSENTE);
    }

    @Test
    void estudianteSoloPuedeVerSusPropiasAsistencias() {
        autenticar("oid-camila", "ESTUDIANTE");
        when(estudianteClient.getEstudianteById(1L)).thenReturn(estudiante(1L, "oid-camila"));
        when(asistenciaClient.searchAsistencias(eq(1L), anyInt())).thenReturn(pagina());

        assertThat(service.getAsistenciasEstudiante(1L)).hasSize(1);

        autenticar("oid-lilith", "ESTUDIANTE");
        when(estudianteClient.getEstudianteById(1L)).thenReturn(estudiante(1L, "oid-camila"));

        assertThatThrownBy(() -> service.getAsistenciasEstudiante(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void apoderadoVinculadoPuedeVerLasAsistenciasDeSuPupilo() {
        autenticar("oid-claudia", "APODERADO");
        when(estudianteClient.getEstudianteById(2L)).thenReturn(estudiante(2L, "oid-lilith"));
        when(apoderadoClient.getApoderadoByIdUsuario("oid-claudia")).thenReturn(new ApoderadoResponseDTO(
            1L, "oid-claudia", "CLAUDIA", "ANDREA", "HERNANDEZ", "MORALES", "44444444-4",
            List.of("+56911111111"),
            List.of(new ParentescoEstudianteDTO(2L, null)),
            true));
        when(asistenciaClient.searchAsistencias(eq(2L), anyInt())).thenReturn(pagina());

        assertThat(service.getAsistenciasEstudiante(2L)).hasSize(1);
    }

    @Test
    void docentePuedeVerLasAsistenciasDeCualquierEstudiante() {
        autenticar("oid-alejandro", "DOCENTE");
        when(estudianteClient.getEstudianteById(3L)).thenReturn(estudiante(3L, "oid-otro"));
        when(asistenciaClient.searchAsistencias(eq(3L), anyInt())).thenReturn(pagina());

        assertThat(service.getAsistenciasEstudiante(3L)).hasSize(1);
    }

    private static void autenticar(String oid, String rol) {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("oid", oid)
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    private static EstudianteResponseDTO estudiante(Long id, String oid) {
        return new EstudianteResponseDTO(
            id, oid, "24112345-6", "CAMILA", "ANTONIETA", "SOTO", "HERNÁNDEZ",
            LocalDate.of(2012, 5, 15), List.of(), "REGISTRADO", 4L);
    }

    private static PageResponseDTO<AsistenciaResponseDTO> pagina() {
        return new PageResponseDTO<>(
            List.of(new AsistenciaResponseDTO(1L, 1L, 3L, LocalDate.of(2026, 10, 2),
                State.AUSENTE, Justificacion.PENDIENTE, null)),
            1, 1, 500, 0, true, true, false);
    }
}

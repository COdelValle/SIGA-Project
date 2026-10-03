package cl.siga.bffweb.domain.apoderados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;

class ApoderadoPupiloServiceTest {

    private final ApoderadoClient apoderadoClient = mock(ApoderadoClient.class);
    private final EstudianteClient estudianteClient = mock(EstudianteClient.class);
    private final ClaseClient claseClient = mock(ClaseClient.class);

    private final ApoderadoPupiloService service = new ApoderadoPupiloService(
        apoderadoClient, estudianteClient, claseClient);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPupilosEnriqueceConCursoYNombre() {
        autenticar("oid-claudia");
        when(apoderadoClient.getApoderadoByIdUsuario("oid-claudia")).thenReturn(new ApoderadoResponseDTO(
            1L, "oid-claudia", "CLAUDIA", "ANDREA", "HERNANDEZ", "MORALES", "44444444-4",
            List.of("+56911111111"),
            List.of(new ParentescoEstudianteDTO(2L, Parentesco.MADRE_PADRE)),
            true));
        when(estudianteClient.getEstudianteById(2L)).thenReturn(estudiante());
        when(claseClient.getClaseById(7L)).thenReturn(new ClaseResponseDTO(7L, Nivel.CUARTO_BASICO, "B", 2026, 14L));

        var pupilos = service.getPupilos();

        assertThat(pupilos).hasSize(1);
        assertThat(pupilos.get(0).nombre()).isEqualTo("LILITH FERNANDA SOTO HERNÁNDEZ");
        assertThat(pupilos.get(0).relacion()).isEqualTo("MADRE_PADRE");
        assertThat(pupilos.get(0).curso()).isEqualTo("4to Básico B");
    }

    @Test
    void actualizarPupiloRechazaNoVinculado() {
        autenticar("oid-claudia");
        when(apoderadoClient.getApoderadoByIdUsuario("oid-claudia")).thenReturn(new ApoderadoResponseDTO(
            1L, "oid-claudia", "CLAUDIA", "ANDREA", "HERNANDEZ", "MORALES", "44444444-4",
            List.of("+56911111111"),
            List.of(new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE)),
            true));

        assertThatThrownBy(() -> service.actualizarPupilo(99L, null))
            .isInstanceOf(AccessDeniedException.class);
    }

    private static void autenticar(String oid) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("oid", oid).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_APODERADO"))));
    }

    private static EstudianteResponseDTO estudiante() {
        return new EstudianteResponseDTO(
            2L, "oid-lilith", "23000021-2", "LILITH", "FERNANDA", "SOTO", "HERNÁNDEZ",
            LocalDate.of(2015, 3, 22), List.of(), "REGISTRADO", 7L);
    }
}

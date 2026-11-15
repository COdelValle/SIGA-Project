package cl.siga.coreshare.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Jwt jwt(String oid, String preferredUsername, String email, String upn) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject");
        if (oid != null) {
            builder.claim("oid", oid);
        }
        if (preferredUsername != null) {
            builder.claim("preferred_username", preferredUsername);
        }
        if (email != null) {
            builder.claim("email", email);
        }
        if (upn != null) {
            builder.claim("upn", upn);
        }
        return builder.build();
    }

    private void autenticar(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void sinAutenticacionNoHayJwtNiOid() {
        assertThat(SecurityUtils.getCurrentJwt()).isEmpty();
        assertThat(SecurityUtils.getCurrentUserOid()).isEmpty();
        assertThat(SecurityUtils.getCurrentUserEmail()).isEmpty();
        assertThat(SecurityUtils.hasRole("ADMIN")).isFalse();
        assertThat(SecurityUtils.isAdmin()).isFalse();
    }

    @Test
    void principalQueNoEsJwtDevuelveVacio() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("usuario", "clave", "ROLE_ADMIN"));

        assertThat(SecurityUtils.getCurrentJwt()).isEmpty();
        assertThat(SecurityUtils.getCurrentUserOid()).isEmpty();
        assertThat(SecurityUtils.getCurrentUserEmail()).isEmpty();
    }

    @Test
    void obtieneOidDelClaim() {
        autenticar(jwt("oid-123", null, null, null));

        assertThat(SecurityUtils.getCurrentUserOid()).contains("oid-123");
    }

    @Test
    void correoUsaPreferredUsernamePrimero() {
        autenticar(jwt("oid-123", "ana@test.com", "otro@test.com", "upn@test.com"));

        assertThat(SecurityUtils.getCurrentUserEmail()).contains("ana@test.com");
    }

    @Test
    void correoUsaEmailSiPreferredUsernameEstaVacio() {
        autenticar(jwt("oid-123", " ", "ana@test.com", "upn@test.com"));

        assertThat(SecurityUtils.getCurrentUserEmail()).contains("ana@test.com");
    }

    @Test
    void correoUsaUpnComoUltimoRecurso() {
        autenticar(jwt("oid-123", null, null, "ana@upn.com"));

        assertThat(SecurityUtils.getCurrentUserEmail()).contains("ana@upn.com");
    }

    @Test
    void correoSinClaimsDevuelveVacio() {
        autenticar(jwt("oid-123", null, null, null));

        assertThat(SecurityUtils.getCurrentUserEmail()).isEmpty();
    }

    @Test
    void hasRoleComparaConPrefijoRole() {
        Jwt jwt = jwt("oid-123", null, null, null);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThat(SecurityUtils.hasRole("ADMIN")).isTrue();
        assertThat(SecurityUtils.hasRole("DOCENTE")).isFalse();
        assertThat(SecurityUtils.isAdmin()).isTrue();
    }

    @Test
    void requireOwnerOrAdminPermiteAlAdmin() {
        Jwt jwt = jwt("admin-oid", null, null, null);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        SecurityUtils.requireOwnerOrAdmin("otro-id", "sin permiso");
    }

    @Test
    void requireOwnerOrAdminPermiteAlDueno() {
        autenticar(jwt("oid-123", null, null, null));

        SecurityUtils.requireOwnerOrAdmin("oid-123", "sin permiso");
    }

    @Test
    void requireOwnerOrAdminRechazaAOtroUsuario() {
        autenticar(jwt("oid-123", null, null, null));

        assertThatThrownBy(() -> SecurityUtils.requireOwnerOrAdmin("otro-id", "sin permiso"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("sin permiso");
    }

    @Test
    void requireOwnerOrAdminSinOidLanzaAccesoDenegado() {
        autenticar(jwt(null, null, null, null));

        assertThatThrownBy(() -> SecurityUtils.requireOwnerOrAdmin("otro-id", "sin permiso"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No se pudo identificar");
    }
}

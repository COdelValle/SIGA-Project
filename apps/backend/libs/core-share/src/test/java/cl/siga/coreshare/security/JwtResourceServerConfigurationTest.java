package cl.siga.coreshare.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtResourceServerConfigurationTest {

    private static final String ISSUER = "https://sts.windows.net/siga-test-tenant/";
    private static final String AUDIENCE = "api://siga-test-api";
    private static final String OTHER_AUDIENCE = "api://other-api";

    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAKey signingKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
            .privateKey((RSAPrivateKey) pair.getPrivate())
            .keyID("siga-test-key")
            .build();
        JWKSource<SecurityContext> keySource = new ImmutableJWKSet<>(new JWKSet(signingKey));
        encoder = new NimbusJwtEncoder(keySource);
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic()).build();
        decoder = JwtResourceServerConfiguration.decoder(nimbus, ISSUER, Set.of(AUDIENCE));
    }

    @Test
    void aceptaTokenFirmadoConIssuerAudienceYVigenciaCorrectos() {
        Jwt decoded = decoder.decode(token(ISSUER, AUDIENCE,
            Instant.now().minusSeconds(30), Instant.now().plusSeconds(300)));

        assertThat(decoded.getSubject()).isEqualTo("oid-de-prueba");
        assertThat(decoded.getAudience()).containsExactly(AUDIENCE);
    }

    @Test
    void rechazaIssuerIncorrecto() {
        assertThatThrownBy(() -> decoder.decode(token(
            "https://login.microsoftonline.com/otro/v2.0", AUDIENCE,
            Instant.now().minusSeconds(30), Instant.now().plusSeconds(300))))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaAudienceIncorrecta() {
        assertThatThrownBy(() -> decoder.decode(token(
            ISSUER, OTHER_AUDIENCE, Instant.now().minusSeconds(30), Instant.now().plusSeconds(300))))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaTokenExpiradoAunqueFirmaIssuerYAudienceSeanCorrectos() {
        assertThatThrownBy(() -> decoder.decode(token(
            ISSUER, AUDIENCE, Instant.now().minusSeconds(600), Instant.now().minusSeconds(300))))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaFirmaAlterada() {
        String[] segments = token(ISSUER, AUDIENCE,
            Instant.now().minusSeconds(30), Instant.now().plusSeconds(300)).split("\\.");
        char first = segments[2].charAt(0) == 'A' ? 'B' : 'A';
        String signatureAlterada = first + segments[2].substring(1);

        assertThatThrownBy(() -> decoder.decode(segments[0] + "." + segments[1] + "." + signatureAlterada))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void mapeaScopesYRolesDeClaimsAAuthoritiesSpring() {
        Instant ahora = Instant.now();
        Jwt jwt = Jwt.withTokenValue("jwt-de-prueba")
            .header("alg", "RS256")
            .subject("oid-de-prueba")
            .issuedAt(ahora.minusSeconds(5))
            .expiresAt(ahora.plusSeconds(60))
            .claim("scp", "usuarios:read estudiantes:read")
            .claim("roles", List.of("ADMIN"))
            .build();

        assertThat(new JwtClaimAuthoritiesConverter().convert(jwt))
            .containsExactlyInAnyOrder(
                new SimpleGrantedAuthority("SCOPE_usuarios:read"),
                new SimpleGrantedAuthority("SCOPE_estudiantes:read"),
                new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private String token(String issuer, String audience, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject("oid-de-prueba")
            .audience(List.of(audience))
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim("scp", "usuarios:read")
            .claim("roles", List.of("ADMIN"))
            .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("siga-test-key").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}

package cl.siga.coreshare.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
    properties = {
        "AZURE_TENANT_ID=siga-test-tenant",
        "AZURE_CLIENT_ID=siga-test-client",
        "AZURE_APP_ID_URI=api://siga-test-api",
        "spring.rabbitmq.dynamic=false"
    })
@AutoConfigureMockMvc
@Import({SharedSecurityFilterTest.TestDecoderConfiguration.class, SharedSecurityFilterTest.SecurityProbeController.class})
class SharedSecurityFilterTest {

    private static final String ISSUER = "https://sts.windows.net/siga-test-tenant/";
    private static final String AUDIENCE = "api://siga-test-api";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rechazaPeticionSinBearer() throws Exception {
        mockMvc.perform(get("/test/security/admin"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void aceptaTokenConFirmaIssuerAudienceRolYScopeValidos() throws Exception {
        mockMvc.perform(get("/test/security/admin")
                .header("Authorization", "Bearer " + token(ISSUER, AUDIENCE, true, true)))
            .andExpect(status().isOk());
    }

    @Test
    void responde403ParaTokenValidoSinScopeRequerido() throws Exception {
        mockMvc.perform(get("/test/security/admin")
                .header("Authorization", "Bearer " + token(ISSUER, AUDIENCE, true, false)))
            .andExpect(status().isForbidden());
    }

    @Test
    void rechazaIssuerAudienceExpiracionYFirmaInvalidos() throws Exception {
        mockMvc.perform(get("/test/security/admin")
                .header("Authorization", "Bearer " + token("https://issuer-invalido/", AUDIENCE, true, true)))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/test/security/admin")
                .header("Authorization", "Bearer " + token(ISSUER, "api://otra-api", true, true)))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/test/security/admin")
                .header("Authorization", "Bearer " + token(ISSUER, AUDIENCE, true, true, Instant.now().minusSeconds(300))))
            .andExpect(status().isUnauthorized());

        String[] segments = token(ISSUER, AUDIENCE, true, true).split("\\.");
        char first = segments[2].charAt(0) == 'A' ? 'B' : 'A';
        String corrupted = segments[0] + "." + segments[1] + "." + first + segments[2].substring(1);
        mockMvc.perform(get("/test/security/admin").header("Authorization", "Bearer " + corrupted))
            .andExpect(status().isUnauthorized());
    }

    private static String token(String issuer, String audience, boolean adminRole, boolean readScope) {
        return token(issuer, audience, adminRole, readScope, Instant.now().plusSeconds(300));
    }

    private static String token(
            String issuer, String audience, boolean adminRole, boolean readScope, Instant expiresAt) {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now) ? expiresAt.minusSeconds(60) : now.minusSeconds(10);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject("oid-test")
            .audience(List.of(audience))
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim("scp", readScope ? "usuarios:read" : "usuarios:write");
        if (adminRole) {
            claims.claim("roles", List.of("ADMIN"));
        }
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("siga-security-test").build();
        return TestKeys.ENCODER.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDecoderConfiguration {
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) TestKeys.KEY_PAIR.getPublic()).build();
            decoder.setJwtValidator(JwtResourceServerConfiguration.validators(ISSUER, Set.of(AUDIENCE)));
            return decoder;
        }
    }

    @RestController
    static class SecurityProbeController {
        @GetMapping(value = "/test/security/admin", produces = MediaType.TEXT_PLAIN_VALUE)
        @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
        String admin() {
            return "ok";
        }
    }

    private static final class TestKeys {
        private static final KeyPair KEY_PAIR = generarPar();
        private static final JwtEncoder ENCODER = crearEncoder(KEY_PAIR);

        private static KeyPair generarPar() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                return generator.generateKeyPair();
            } catch (Exception error) {
                throw new ExceptionInInitializerError(error);
            }
        }

        private static JwtEncoder crearEncoder(KeyPair keyPair) {
            try {
                RSAKey signingKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID("siga-security-test")
                    .build();
                JWKSource<SecurityContext> keys = new ImmutableJWKSet<>(new JWKSet(signingKey));
                return new NimbusJwtEncoder(keys);
            } catch (Exception error) {
                throw new ExceptionInInitializerError(error);
            }
        }
    }
}

package cl.siga.coreshare.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/** Configuración explícita del Resource Server Entra ID compartido por los servicios. */
@AutoConfiguration
public class JwtResourceServerConfiguration {

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder sigaJwtDecoder(Environment environment) {
        String tenantId = environment.getRequiredProperty("AZURE_TENANT_ID");
        String appIdUri = environment.getRequiredProperty("AZURE_APP_ID_URI");
        String clientId = environment.getRequiredProperty("AZURE_CLIENT_ID");
        String issuer = nonBlank(environment.getProperty("AZURE_JWT_ISSUER"),
            "https://sts.windows.net/" + tenantId + "/");
        String jwkSetUri = nonBlank(environment.getProperty("AZURE_JWKS_URI"),
            "https://login.microsoftonline.com/" + tenantId + "/discovery/keys");
        String configuredAudiences = nonBlank(environment.getProperty("AZURE_JWT_AUDIENCES"),
            appIdUri + "," + clientId);
        Set<String> audiences = Arrays.stream(configuredAudiences.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(LinkedHashSet::new, Set::add, Set::addAll);

        return decoder(NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build(), issuer, audiences);
    }

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter sigaJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtClaimAuthoritiesConverter());
        return converter;
    }

    static NimbusJwtDecoder decoder(NimbusJwtDecoder decoder, String issuer, Set<String> audiences) {
        decoder.setJwtValidator(validators(issuer, audiences));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validators(String issuer, Set<String> audiences) {
        return new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            new JwtAudienceValidator(audiences));
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

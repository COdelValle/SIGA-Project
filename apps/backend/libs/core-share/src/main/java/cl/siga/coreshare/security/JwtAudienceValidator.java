package cl.siga.coreshare.security;

import java.util.Set;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Valida que el access token esté emitido para una audiencia permitida de SIGA. */
public final class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final Set<String> audiences;

    public JwtAudienceValidator(Set<String> audiences) {
        this.audiences = Set.copyOf(audiences);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        boolean accepted = jwt.getAudience().stream().anyMatch(audiences::contains);
        if (accepted) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
            "invalid_token", "El token no está emitido para una audiencia de SIGA.", null));
    }
}

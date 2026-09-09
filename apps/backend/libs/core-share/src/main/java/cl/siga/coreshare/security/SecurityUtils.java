package cl.siga.coreshare.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    /**
     * Obtiene el Object ID (oid) de Entra ID / Azure AD.
     * Este es el identificador global del usuario en Microsoft.
     */
    public static Optional<String> getCurrentUserOid() {
        return getCurrentJwt().map(jwt -> jwt.getClaimAsString("oid"));
    }

    /**
     * Obtiene el email o nombre de usuario principal.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentJwt().map(jwt -> {
            String email = jwt.getClaimAsString("preferred_username");
            return email != null ? email : jwt.getClaimAsString("email");
        });
    }
}
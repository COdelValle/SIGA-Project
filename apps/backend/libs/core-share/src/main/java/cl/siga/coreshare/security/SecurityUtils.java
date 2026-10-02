package cl.siga.coreshare.security;

import org.springframework.security.access.AccessDeniedException;
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

    /** Indica si el usuario autenticado tiene el rol indicado (sin el prefijo ROLE_). */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }

    /** Indica si el usuario autenticado es ADMIN. */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Exige que el usuario autenticado sea ADMIN o el dueno del recurso
     * (comparando el claim oid con el idUsuario de la entidad).
     */
    public static void requireOwnerOrAdmin(String idUsuario, String mensaje) {
        if (isAdmin()) {
            return;
        }
        String oid = getCurrentUserOid()
                .orElseThrow(() -> new AccessDeniedException("No se pudo identificar al usuario autenticado."));
        if (!oid.equals(idUsuario)) {
            throw new AccessDeniedException(mensaje);
        }
    }
}
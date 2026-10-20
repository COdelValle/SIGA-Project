package cl.siga.coreshare.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Convierte los claims delegados {@code scp} y de aplicación {@code roles}. */
public final class JwtClaimAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Object scopesClaim = jwt.getClaims().get("scp");
        if (scopesClaim instanceof String scopes) {
            for (String scope : scopes.split("\\s+")) {
                if (!scope.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                }
            }
        }

        Object rolesClaim = jwt.getClaims().get("roles");
        if (rolesClaim instanceof Collection<?> roles) {
            for (Object role : roles) {
                if (role instanceof String value && !value.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + value));
                }
            }
        }
        return authorities;
    }
}

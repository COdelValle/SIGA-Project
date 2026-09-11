package cl.siga.coreshare.security;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadResourceServerHttpSecurityConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration 
@EnableWebSecurity
@EnableMethodSecurity
public class SharedSecurityConfig {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain sharedSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/webjars/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/docs/swagger",
                    "/docs/swagger/**",
                    "/docs/swagger-ui/**",
                    "/scalar",
                    "/scalar/**",
                    "/docs/scalar/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // Aplica automáticamente las reglas de validación y roles de Azure AD
            .with(AadResourceServerHttpSecurityConfigurer.aadResourceServer(), Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean (JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();

            // 1. Extracción de SCOPES (Mapea a "SCOPE_nombre")
            String scpClaim = jwt.getClaimAsString("scp");
            if (scpClaim != null && !scpClaim.isBlank()) {
                // Maneja el formato OAuth2 estándar: "read write delete"
                Arrays.stream(scpClaim.split(" "))
                    .forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
            } else {
                // Fallback si vienen como una lista JSON: ["read", "write"]
                List<String> scopesList = jwt.getClaimAsStringList("scopes");
                if (scopesList != null) {
                    scopesList.forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
                }
            }

            // 2. Extracción de ROLES (Mapea a "ROLE_nombre")
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }

            return authorities;
        });

        return jwtConverter;
    }
}
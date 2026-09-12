package cl.siga.coreshare.security;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadResourceServerHttpSecurityConfigurer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
            // Aplica las reglas de validación de Azure AD. El mapeo de claims a
            // authorities (scp -> SCOPE_, roles -> ROLE_) se define en
            // common-properties.yaml (claim-to-authority-prefix-map).
            .with(AadResourceServerHttpSecurityConfigurer.aadResourceServer(), Customizer.withDefaults());

        return http.build();
    }
}

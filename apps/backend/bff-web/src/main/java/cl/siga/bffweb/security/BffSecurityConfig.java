package cl.siga.bffweb.security;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadResourceServerHttpSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class BffSecurityConfig {

    private final UserStatusFilter userStatusFilter;

    public BffSecurityConfig(UserStatusFilter userStatusFilter) {
        this.userStatusFilter = userStatusFilter;
    }

    @Bean
    public SecurityFilterChain bffFilterChain(HttpSecurity http) throws Exception {
        http
            .with(AadResourceServerHttpSecurityConfigurer.aadResourceServer(), Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            // Agrega el filtro de validación en Redis solo para las peticiones que pasan por el BFF
            .addFilterAfter(userStatusFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
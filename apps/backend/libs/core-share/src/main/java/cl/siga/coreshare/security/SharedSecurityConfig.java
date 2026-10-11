package cl.siga.coreshare.security;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadResourceServerHttpSecurityConfigurer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
                // Preflight CORS: OPTIONS es idempotente y no expone datos. Se
                // permite para que el navegador reciba 2xx en el preflight
                // (API Gateway reenvia el OPTIONS al BFF por la ruta explicita
                // sin autorizacion); los metodos reales siguen autenticados.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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

    /**
     * CORS global de Spring MVC para el preflight: API Gateway reenvia el
     * OPTIONS al servicio (ruta explicita sin autorizacion) y el handler
     * mapping debe responderlo; sin configuracion global rechaza con 403
     * "Invalid CORS request". La allowlist efectiva la impone API Gateway
     * (ignora los headers CORS del backend); aqui no hay credenciales y los
     * metodos reales siguen autenticados.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("Authorization", "Content-Type")
                    .allowCredentials(false)
                    .maxAge(300);
            }
        };
    }
}

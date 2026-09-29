package cl.siga.msevaluaciones.config;

import cl.siga.coreshare.security.SecurityUtils;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> SecurityUtils.getCurrentJwt()
                .ifPresent(jwt -> template.header("Authorization", "Bearer " + jwt.getTokenValue()));
    }
}

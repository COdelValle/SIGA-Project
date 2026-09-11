package cl.siga.msnotas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cl.siga.coreshare.security.SecurityUtils;
import feign.RequestInterceptor;

@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> SecurityUtils.getCurrentJwt()
                .ifPresent(jwt -> template.header("Authorization", "Bearer " + jwt.getTokenValue()));
    }
}

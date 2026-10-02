package cl.siga.coreshare.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import cl.siga.coreshare.security.SecurityUtils;
import feign.RequestInterceptor;

/**
 * Propaga el token JWT entrante en las llamadas Feign de todos los servicios.
 * Si el servicio define su propio {@link RequestInterceptor} (por ejemplo el BFF),
 * este se omite para no duplicar el header Authorization.
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class SharedFeignAuthConfig {

    @Bean
    @ConditionalOnMissingBean(RequestInterceptor.class)
    public RequestInterceptor authForwardingInterceptor() {
        return template -> SecurityUtils.getCurrentJwt()
                .ifPresent(jwt -> template.header("Authorization", "Bearer " + jwt.getTokenValue()));
    }
}

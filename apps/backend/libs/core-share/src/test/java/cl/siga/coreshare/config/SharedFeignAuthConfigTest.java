package cl.siga.coreshare.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import feign.RequestInterceptor;
import feign.RequestTemplate;

class SharedFeignAuthConfigTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    private void autenticar(String token) {
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("oid")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void propagaAuthorizationYCorrelationId() {
        autenticar("jwt-123");
        MDC.put(CorrelationIdConfig.MDC_KEY, "corr-1");
        RequestTemplate template = mock(RequestTemplate.class);

        RequestInterceptor interceptor = new SharedFeignAuthConfig().authForwardingInterceptor();
        interceptor.apply(template);

        verify(template).header(eq("Authorization"), eq("Bearer jwt-123"));
        verify(template).header(eq(CorrelationIdConfig.HEADER), eq("corr-1"));
    }

    @Test
    void sinJwtNiCorrelationIdNoAgregaHeaders() {
        RequestTemplate template = mock(RequestTemplate.class);

        RequestInterceptor interceptor = new SharedFeignAuthConfig().authForwardingInterceptor();
        interceptor.apply(template);

        verify(template, never()).header(any(String.class), any(String[].class));
    }

    @Test
    void sinJwtPeroConCorrelationIdPropagaSoloElCorrelationId() {
        MDC.put(CorrelationIdConfig.MDC_KEY, "corr-2");
        RequestTemplate template = mock(RequestTemplate.class);

        RequestInterceptor interceptor = new SharedFeignAuthConfig().authForwardingInterceptor();
        interceptor.apply(template);

        verify(template).header(eq(CorrelationIdConfig.HEADER), eq("corr-2"));
        verify(template, never()).header(eq("Authorization"), any(String[].class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

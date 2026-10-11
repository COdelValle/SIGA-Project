package cl.siga.coreshare.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Correlation ID por peticion: lee o genera {@code X-Correlation-Id}, lo publica
 * en el MDC (para los logs) y lo devuelve en la respuesta.
 */
@AutoConfiguration
public class CorrelationIdConfig {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    public static class CorrelationIdFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            String correlationId = request.getHeader(HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_KEY, correlationId);
            response.setHeader(HEADER, correlationId);
            try {
                filterChain.doFilter(request, response);
            } finally {
                MDC.remove(MDC_KEY);
            }
        }
    }
}

package cl.siga.bffweb.security;

import cl.siga.bffweb.service.UserStatusService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserStatusFilter extends OncePerRequestFilter {

    private final UserStatusService userStatusService;

    public UserStatusFilter(UserStatusService userStatusService) {
        this.userStatusService = userStatusService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Verificar si la petición viene con un Token JWT válido
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String providerId = jwt.getClaimAsString("oid"); // ID de Microsoft

            if (providerId != null) {
                String estado = userStatusService.obtenerEstadoUsuario(providerId);

                if ("SUSPENDIDO".equalsIgnoreCase(estado)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Acceso denegado. Su cuenta local se encuentra suspendida.\"}");
                    return; // Bloquea la petición de inmediato
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
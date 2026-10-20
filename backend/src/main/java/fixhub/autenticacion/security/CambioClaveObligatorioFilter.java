package fixhub.autenticacion.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CambioClaveObligatorioFilter
    extends OncePerRequestFilter {

    private static final String CAMBIO_CLAVE_PATH =
        "/api/auth/cambiar-clave";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();

        if (
            requiereCambioClave(authentication)
                && !esSolicitudPermitida(request)
        ) {
            responderAccesoDenegado(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiereCambioClave(
        Authentication authentication
    ) {
        if (
            authentication
                instanceof JwtAuthenticationToken jwtAuthentication
        ) {
            Boolean requiereCambio = jwtAuthentication
                .getToken()
                .getClaimAsBoolean("requiereCambioClave");

            return Boolean.TRUE.equals(requiereCambio);
        }

        return false;
    }

    private boolean esSolicitudPermitida(
        HttpServletRequest request
    ) {
        return HttpMethod.OPTIONS.matches(request.getMethod())
            || CAMBIO_CLAVE_PATH.equals(request.getRequestURI());
    }

    private void responderAccesoDenegado(
        HttpServletResponse response
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
        );
        response.setContentType(
            MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );

        response.getWriter().write("""
            {
                "type": "about:blank",
                "title": "Cambio de clave requerido",
                "status": 403,
                "detail": "Debe cambiar la clave temporal antes de continuar"
            }
            """);
    }
}

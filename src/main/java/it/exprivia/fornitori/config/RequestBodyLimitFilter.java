package it.exprivia.fornitori.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Il JSON delle API è piccolo e il login è pubblico: un corpo enorme non deve arrivare al parser.
 * Sta prima della catena di sicurezza. Senza lunghezza dichiarata il limite non sarebbe verificabile.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
class RequestBodyLimitFilter extends OncePerRequestFilter {
    static final long MAX_BODY_BYTES = 64 * 1024;
    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !BODY_METHODS.contains(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            reject(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "payload_too_large");
            return;
        }
        if (request.getContentLengthLong() < 0 && request.getHeader("Transfer-Encoding") != null) {
            reject(response, HttpServletResponse.SC_LENGTH_REQUIRED, "length_required");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static void reject(HttpServletResponse response, int status, String error) throws IOException {
        response.setStatus(status);
        response.setHeader("Connection", "close");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error + "\"}");
    }
}

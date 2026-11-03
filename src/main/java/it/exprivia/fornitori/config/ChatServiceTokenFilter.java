package it.exprivia.fornitori.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Autentica il chatbot con il token di servizio; senza token configurato non autentica nessuno. */
class ChatServiceTokenFilter extends OncePerRequestFilter {
    static final String ROLE = "SERVICE_CHATBOT";
    private static final String BEARER_PREFIX = "Bearer ";

    private final byte[] expectedToken;

    ChatServiceTokenFilter(String serviceToken) {
        this.expectedToken = serviceToken == null ? new byte[0] : serviceToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isAuthorized(request.getHeader("Authorization"))) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    "chatbot", null, List.of(new SimpleGrantedAuthority("ROLE_" + ROLE))));
            SecurityContextHolder.setContext(context);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAuthorized(String header) {
        if (expectedToken.length == 0 || header == null || !header.startsWith(BEARER_PREFIX)) {
            return false;
        }
        byte[] provided = header.substring(BEARER_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedToken, provided);
    }
}

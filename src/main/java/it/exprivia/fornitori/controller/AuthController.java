package it.exprivia.fornitori.controller;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.exprivia.fornitori.dto.AuthDtos;
import it.exprivia.fornitori.common.ApiError;
import it.exprivia.fornitori.service.RequestRateLimiter;
import it.exprivia.fornitori.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final int MAX_LOGINS_PER_MINUTE_BY_ADDRESS = 20;
    private static final int MAX_FAILURES_PER_MINUTE_BY_ACCOUNT = 10;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuthService authService;
    private final CsrfTokenRepository csrfTokens;
    private final RequestRateLimiter rateLimiter;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
    private final CookieClearingLogoutHandler cookieHandler = new CookieClearingLogoutHandler("JSESSIONID");

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            AuthService authService,
            CsrfTokenRepository csrfTokens,
            RequestRateLimiter rateLimiter) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.authService = authService;
        this.csrfTokens = csrfTokens;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication;
        rateLimiter.check("login:" + servletRequest.getRemoteAddr(), MAX_LOGINS_PER_MINUTE_BY_ADDRESS);
        // Conta solo i falliti: un attacco da molti indirizzi non blocca chi conosce la propria password più del dovuto.
        String accountKey = "login-failure:" + request.email().toLowerCase(Locale.ROOT);
        rateLimiter.requireBelow(accountKey, MAX_FAILURES_PER_MINUTE_BY_ACCOUNT);
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        } catch (AuthenticationException exception) {
            rateLimiter.record(accountKey);
            logger.warn("Login fallito dall'indirizzo {}", servletRequest.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(401, "unauthorized"));
        }

        HttpSession existingSession = servletRequest.getSession(false);
        if (existingSession != null) {
            servletRequest.changeSessionId();
        }
        csrfTokens.saveToken(null, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return ResponseEntity.ok(authService.currentUser(authentication.getName()));
    }

    @GetMapping("/me")
    public AuthDtos.CurrentUser me(Authentication authentication) {
        return authService.currentUser(authentication.getName());
    }

    @GetMapping("/csrf")
    public AuthDtos.CsrfTokenResponse csrf(CsrfToken token) {
        return new AuthDtos.CsrfTokenResponse(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        logoutHandler.logout(request, response, authentication);
        cookieHandler.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

}

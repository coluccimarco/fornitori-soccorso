package it.exprivia.fornitori.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
        @Override public String toString() { return "LoginRequest[redacted]"; }
    }

    public record CurrentUser(Long id, String nome, String email, String ruolo, Long autistaId) {
    }

    public record CsrfTokenResponse(String token, String headerName) {
        @Override public String toString() { return "CsrfTokenResponse[redacted]"; }
    }
}

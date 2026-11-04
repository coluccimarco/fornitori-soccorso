package it.exprivia.fornitori.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Errore pubblico: codice stabile e, se disponibile, un testo scritto dal servizio. Mai eccezioni o SQL. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String error, String message) {
    public ApiError(int status, String error) {
        this(status, error, null);
    }
}

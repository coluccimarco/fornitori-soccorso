package it.exprivia.fornitori.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ForbiddenOperationException extends ResponseStatusException {
    public ForbiddenOperationException() {
        super(HttpStatus.FORBIDDEN, "Operation not authorized");
    }
}

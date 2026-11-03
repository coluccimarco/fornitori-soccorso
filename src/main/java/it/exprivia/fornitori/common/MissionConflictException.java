package it.exprivia.fornitori.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MissionConflictException extends ResponseStatusException {
    public MissionConflictException(String reason) {
        super(HttpStatus.CONFLICT, reason);
    }
}

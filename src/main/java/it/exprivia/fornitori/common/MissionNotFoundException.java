package it.exprivia.fornitori.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MissionNotFoundException extends ResponseStatusException {
    public MissionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Mission not found");
    }
}

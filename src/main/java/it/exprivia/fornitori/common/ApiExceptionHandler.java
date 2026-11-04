package it.exprivia.fornitori.common;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> invalidParameter(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiError(400, "invalid_request"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> wrongMethod(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(405).body(new ApiError(405, "method_not_allowed"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> wrongMediaType(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(415).body(new ApiError(415, "unsupported_media_type"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new ApiError(400, "invalid_request"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ApiError(400, "invalid_request"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> illegalState(IllegalStateException exception) {
        logger.error("Inconsistent application state while handling a request", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "internal_error"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> dataConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(409, "conflict"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> responseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String error = switch (status) {
            case 400 -> "invalid_request";
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "not_found";
            case 409 -> "conflict";
            case 422 -> "address_not_found";
            case 429 -> "rate_limited";
            case 503 -> "service_unavailable";
            default -> "internal_error";
        };
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiError(status, error));
    }
}

package it.exprivia.fornitori.common;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);
    /** Solo questi stati riportano il motivo: i service li scrivono per l'utente, i 503 possono citare i provider. */
    private static final List<Integer> STATUSES_WITH_MESSAGE = List.of(400, 404, 409, 422);

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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> dataConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(409, "conflict"));
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiError> concurrencyConflict(ConcurrencyFailureException exception) {
        logger.warn("Conflitto di concorrenza sul database: {}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(409, "conflict"));
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ApiError> databaseUnavailable(Exception exception) {
        logger.error("Database non raggiungibile: {}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(503, "service_unavailable"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> responseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = STATUSES_WITH_MESSAGE.contains(status) ? exception.getReason() : null;
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiError(status, publicCode(status), message));
    }

    /** Ultima rete: senza, l'errore finirebbe su /error, che la sicurezza nega, e il client leggerebbe 401 o 403. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception) throws Exception {
        if (exception instanceof AccessDeniedException || exception instanceof AuthenticationException) {
            throw exception;
        }
        if (exception instanceof ErrorResponse framework) {
            int status = framework.getStatusCode().value();
            return ResponseEntity.status(status).body(new ApiError(status, publicCode(status)));
        }
        String reference = UUID.randomUUID().toString().substring(0, 8);
        logger.error("Errore inatteso, riferimento {}", reference, exception);
        return ResponseEntity.internalServerError()
                .body(new ApiError(500, "internal_error", "Errore interno, riferimento " + reference));
    }

    private static String publicCode(int status) {
        return switch (status) {
            case 400 -> "invalid_request";
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "not_found";
            case 405 -> "method_not_allowed";
            case 409 -> "conflict";
            case 415 -> "unsupported_media_type";
            case 422 -> "address_not_found";
            case 429 -> "rate_limited";
            case 503 -> "service_unavailable";
            default -> status < 500 ? "invalid_request" : "internal_error";
        };
    }
}

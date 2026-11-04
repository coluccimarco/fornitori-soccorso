package it.exprivia.fornitori.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Limita le targhe non trovate per conversazione, così il bot non si presta a provarle tutte. */
@Component
public class ChatLookupLimiter {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public ChatLookupLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String sessionId) {
        Instant now = clock.instant();
        failures.values().removeIf(entry -> !now.isBefore(entry.expiresAt()));
        Failures entry = failures.get(sessionId);
        return entry != null && entry.count() >= MAX_FAILURES;
    }

    public void recordFailure(String sessionId) {
        Instant now = clock.instant();
        failures.merge(sessionId, new Failures(1, now.plus(WINDOW)),
                (current, ignored) -> new Failures(current.count() + 1, current.expiresAt()));
    }

    private record Failures(int count, Instant expiresAt) {
    }
}

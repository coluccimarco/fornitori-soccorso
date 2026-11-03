package it.exprivia.fornitori.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Limita i tentativi di verifica per targa: vale anche per le targhe inesistenti,
 * così il blocco non rivela quali targhe sono assicurate. Lo stato resta in memoria.
 */
@Component
public class ChatLookupLimiter {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public ChatLookupLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String plate) {
        Instant now = clock.instant();
        failures.values().removeIf(entry -> !now.isBefore(entry.expiresAt()));
        Failures entry = failures.get(plate);
        return entry != null && entry.count() >= MAX_FAILURES;
    }

    public void recordFailure(String plate) {
        Instant now = clock.instant();
        failures.merge(plate, new Failures(1, now.plus(WINDOW)),
                (current, ignored) -> new Failures(current.count() + 1, current.expiresAt()));
    }

    public void reset(String plate) {
        failures.remove(plate);
    }

    private record Failures(int count, Instant expiresAt) {
    }
}

package it.exprivia.fornitori.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Bounded, single-instance fixed window. Does not trust forwarded client headers. */
@Component
public class RequestRateLimiter {
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();
    public RequestRateLimiter(Clock clock) { this.clock = clock; }

    public synchronized void check(String key, int limit) {
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> !entry.getValue().until.isAfter(now));
        Window window = windows.get(key);
        if (window == null) {
            if (windows.size() >= 10000) throw limited();
            window = new Window(now.plusSeconds(60));
            windows.put(key, window);
        }
        if (window.count >= limit) throw limited();
        window.count++;
    }

    private static ResponseStatusException limited() {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
    }

    private static final class Window {
        private final Instant until;
        private int count;
        private Window(Instant until) { this.until = until; }
    }
}

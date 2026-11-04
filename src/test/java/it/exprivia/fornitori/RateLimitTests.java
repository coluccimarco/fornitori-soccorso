package it.exprivia.fornitori;

import it.exprivia.fornitori.service.RequestRateLimiter;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;

class RateLimitTests {
    @Test void limitIsAtomicAndExpiresWithClock() {
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-11-04T10:00:00Z");
        when(clock.instant()).thenReturn(start);
        RequestRateLimiter limiter = new RequestRateLimiter(clock);
        for (int i = 0; i < 20; i++) limiter.check("login:127.0.0.1", 20);
        assertEquals(429, assertThrows(ResponseStatusException.class,
                () -> limiter.check("login:127.0.0.1", 20)).getStatusCode().value());
        limiter.check("login:another", 20);
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        assertDoesNotThrow(() -> limiter.check("login:127.0.0.1", 20));
    }
}

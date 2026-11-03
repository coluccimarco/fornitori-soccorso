package it.exprivia.fornitori;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.exprivia.fornitori.service.ChatLookupLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ChatSupportTests {
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-03T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();

    @Test
    void limiterBlocksAfterFiveFailuresUntilWindowEnds() {
        ChatLookupLimiter limiter = new ChatLookupLimiter(clock);
        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(limiter.isBlocked("sessione-a"));
            limiter.recordFailure("sessione-a");
        }
        assertTrue(limiter.isBlocked("sessione-a"));
        assertFalse(limiter.isBlocked("sessione-b"));
        clock.advance(Duration.ofMinutes(15));
        assertFalse(limiter.isBlocked("sessione-a"));
    }

    @Test
    void recognizedPlateResetsFailures() {
        ChatLookupLimiter limiter = new ChatLookupLimiter(clock);
        for (int attempt = 0; attempt < 4; attempt++) {
            limiter.recordFailure("sessione-a");
        }
        limiter.reset("sessione-a");
        limiter.recordFailure("sessione-a");
        assertFalse(limiter.isBlocked("sessione-a"));
    }
}

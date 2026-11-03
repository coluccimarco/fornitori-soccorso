package it.exprivia.fornitori;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.exprivia.fornitori.service.ChatLookupLimiter;
import it.exprivia.fornitori.service.ChatVerificationTokens;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
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
    private final ChatVerificationTokens tokens = new ChatVerificationTokens("chiave-di-prova", 60, clock);

    @Test
    void tokenIdentifiesCustomerUntilExpiry() {
        String token = tokens.issue(7L);
        assertEquals(Optional.of(7L), tokens.verify(token));
        clock.advance(Duration.ofMinutes(59));
        assertEquals(Optional.of(7L), tokens.verify(token));
        clock.advance(Duration.ofMinutes(1));
        assertEquals(Optional.empty(), tokens.verify(token));
    }

    @Test
    void forgedOrMalformedTokensAreRejected() {
        String token = tokens.issue(7L);
        String otherKey = new ChatVerificationTokens("altra-chiave", 60, clock).issue(7L);
        String forgedPayload = new ChatVerificationTokens("chiave-di-prova", 60, clock).issue(8L).split("\\.")[0]
                + "." + token.split("\\.")[1];
        assertEquals(Optional.empty(), tokens.verify(otherKey));
        assertEquals(Optional.empty(), tokens.verify(forgedPayload));
        assertEquals(Optional.empty(), tokens.verify("non-valido"));
        assertEquals(Optional.empty(), tokens.verify("%%%.%%%"));
        assertEquals(Optional.empty(), tokens.verify(null));
    }

    @Test
    void withoutKeyNoTokenIsAccepted() {
        ChatVerificationTokens unconfigured = new ChatVerificationTokens("", 60, clock);
        assertEquals(Optional.empty(), unconfigured.verify(tokens.issue(7L)));
    }

    @Test
    void limiterBlocksAfterFiveFailuresUntilWindowEnds() {
        ChatLookupLimiter limiter = new ChatLookupLimiter(clock);
        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(limiter.isBlocked("AB123CD"));
            limiter.recordFailure("AB123CD");
        }
        assertTrue(limiter.isBlocked("AB123CD"));
        assertFalse(limiter.isBlocked("EF456GH"));
        clock.advance(Duration.ofMinutes(15));
        assertFalse(limiter.isBlocked("AB123CD"));
    }

    @Test
    void successfulVerificationResetsFailures() {
        ChatLookupLimiter limiter = new ChatLookupLimiter(clock);
        for (int attempt = 0; attempt < 4; attempt++) {
            limiter.recordFailure("AB123CD");
        }
        limiter.reset("AB123CD");
        limiter.recordFailure("AB123CD");
        assertFalse(limiter.isBlocked("AB123CD"));
    }
}

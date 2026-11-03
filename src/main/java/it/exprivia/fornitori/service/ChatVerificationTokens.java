package it.exprivia.fornitori.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Token firmati che attestano quale cliente il chatbot ha verificato e fino a quando. */
@Component
public class ChatVerificationTokens {
    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] key;
    private final long ttlSeconds;
    private final Clock clock;

    public ChatVerificationTokens(
            @Value("${fornitori.chat.service-token:}") String serviceToken,
            @Value("${fornitori.chat.verification-token-ttl-minutes:60}") long ttlMinutes,
            Clock clock) {
        this.key = serviceToken.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlMinutes * 60;
        this.clock = clock;
    }

    public String issue(Long customerId) {
        String payload = customerId + ":" + (clock.instant().getEpochSecond() + ttlSeconds);
        return encode(payload.getBytes(StandardCharsets.UTF_8)) + "." + encode(sign(payload));
    }

    public Optional<Long> verify(String token) {
        if (token == null || key.length == 0) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            byte[] signature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(sign(payload), signature)) {
                return Optional.empty();
            }
            String[] fields = payload.split(":");
            if (Long.parseLong(fields[1]) <= clock.instant().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(fields[0]));
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException exception) {
            return Optional.empty();
        }
    }

    private byte[] sign(String payload) {
        if (key.length == 0) {
            throw new IllegalStateException("Chat service token is not configured");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot sign chat verification token", exception);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}

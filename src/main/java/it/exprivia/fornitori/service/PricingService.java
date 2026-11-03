package it.exprivia.fornitori.service;

import it.exprivia.fornitori.entity.PricingConfig;
import it.exprivia.fornitori.repository.PricingConfigRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PricingService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Rome");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal CENTS_SCALE = BigDecimal.valueOf(2);

    private final PricingConfigRepository configurations;

    public PricingService(PricingConfigRepository configurations) {
        this.configurations = configurations;
    }

    public Price calculate(BigDecimal kilometers, Instant acceptedAt) {
        if (kilometers == null || kilometers.signum() < 0) {
            throw new IllegalArgumentException("Kilometers must be non-negative");
        }
        Objects.requireNonNull(acceptedAt, "acceptedAt");
        PricingConfig config = configurations.findById((short) 1)
                .orElseThrow(() -> new IllegalStateException("Pricing configuration id 1 is missing"));
        LocalTime localAcceptanceTime = acceptedAt.atZone(BUSINESS_ZONE).toLocalTime();
        boolean night = !localAcceptanceTime.isBefore(config.getNightStart())
                || localAcceptanceTime.isBefore(config.getNightEnd());
        BigDecimal base = config.getUscitaEur()
                .add(config.getTariffaKmEur().multiply(kilometers));
        BigDecimal multiplier = night
                ? ONE.add(config.getMaggiorazioneNotturna())
                : ONE;
        return new Price(base.multiply(multiplier).setScale(CENTS_SCALE.intValue(), RoundingMode.HALF_UP), night);
    }

    public record Price(BigDecimal amount, boolean night) {
    }
}

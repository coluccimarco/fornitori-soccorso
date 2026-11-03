package it.exprivia.fornitori;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.exprivia.fornitori.entity.PricingConfig;
import it.exprivia.fornitori.repository.PricingConfigRepository;
import it.exprivia.fornitori.service.PricingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingServiceTests {
    private PricingService pricing;

    @BeforeEach
    void setUp() {
        PricingConfigRepository configurations = mock(PricingConfigRepository.class);
        PricingConfig config = mock(PricingConfig.class);
        when(config.getUscitaEur()).thenReturn(new BigDecimal("50.00"));
        when(config.getTariffaKmEur()).thenReturn(new BigDecimal("1.20"));
        when(config.getMaggiorazioneNotturna()).thenReturn(new BigDecimal("0.2500"));
        when(config.getNightStart()).thenReturn(java.time.LocalTime.of(20, 0));
        when(config.getNightEnd()).thenReturn(java.time.LocalTime.of(8, 0));
        when(configurations.findById((short) 1)).thenReturn(Optional.of(config));
        pricing = new PricingService(configurations);
    }

    @Test
    void calculatesExcelDayAndNightExamplesInRomeTime() {
        assertPrice("18", "2026-11-03T10:00:00Z", "71.60", false);
        assertPrice("18", "2026-11-03T21:30:00Z", "89.50", true);
        assertPrice("7.5", "2026-11-03T06:45:00Z", "73.75", true);
        assertPrice("10", "2026-11-03T19:00:00Z", "77.50", true);
        assertPrice("10", "2026-11-03T07:00:00Z", "62.00", false);
    }

    private void assertPrice(String km, String acceptedAt, String expected, boolean night) {
        PricingService.Price result = pricing.calculate(new BigDecimal(km), Instant.parse(acceptedAt));
        assertEquals(new BigDecimal(expected), result.amount());
        assertEquals(night, result.night());
    }
}

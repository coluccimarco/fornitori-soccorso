package it.exprivia.fornitori.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Contratti dei servizi esterni di mappe, sostituibili nei test. */
public final class MapProviders {
    private MapProviders() {
    }

    public record GeoPoint(BigDecimal lat, BigDecimal lng) {
    }

    public record RouteResult(BigDecimal km, Map<String, Object> geometry) {
    }

    public interface GeocodingClient {
        Optional<GeoPoint> geocode(String address);
    }

    public interface RoutingClient {
        RouteResult route(List<GeoPoint> points);
    }

    /** Il servizio esterno non risponde o non trova un percorso: la chiusura missione non ne dipende. */
    public static class ProviderUnavailableException extends RuntimeException {
        public ProviderUnavailableException(String message) {
            super(message);
        }
    }
}

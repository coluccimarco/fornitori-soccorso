package it.exprivia.fornitori.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Contratti dei servizi esterni di mappe, sostituibili con altri provider. */
public final class MapProviders {
    private MapProviders() {
    }

    public record GeoPoint(BigDecimal lat, BigDecimal lng) {
        public GeoPoint {
            if (lat == null || lng == null || lat.abs().compareTo(BigDecimal.valueOf(90)) > 0
                    || lng.abs().compareTo(BigDecimal.valueOf(180)) > 0)
                throw new ProviderUnavailableException("Coordinate provider non valide");
        }
    }

    /** legsKm ha un valore per ogni tratto tra due punti consecutivi; vuota se il provider non li distingue. */
    public record RouteResult(BigDecimal km, Map<String, Object> geometry, List<BigDecimal> legsKm) {
        public RouteResult {
            if (km == null || km.signum() < 0 || km.compareTo(new BigDecimal("99999999.99")) > 0 || geometry == null
                    || legsKm == null || legsKm.stream().anyMatch(leg -> leg == null || leg.signum() < 0))
                throw new ProviderUnavailableException("Percorso provider non valido");
        }

        public RouteResult(BigDecimal km, Map<String, Object> geometry) {
            this(km, geometry, List.of());
        }
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

package it.exprivia.fornitori.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.exprivia.fornitori.service.MapProviders.GeoPoint;
import it.exprivia.fornitori.service.MapProviders.ProviderUnavailableException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/** Geocodifica con il servizio pubblico Nominatim, che chiede un User-Agent e al massimo una richiesta al secondo. */
@Component
public class NominatimGeocodingClient implements MapProviders.GeocodingClient {
    private static final long MIN_INTERVAL_MILLIS = 1_000;

    private final String baseUrl;
    private final RestClient client;
    private long lastRequestAt;

    /** Puglia: senza limite Nominatim può rispondere con una via omonima dall'altra parte d'Italia. */
    static final String DEFAULT_VIEWBOX = "15.8,42.2,18.6,39.7";
    private static final Pattern VIEWBOX = Pattern.compile(
            "-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?");

    private final String viewbox;

    public NominatimGeocodingClient(
            @Value("${fornitori.maps.nominatim-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${fornitori.maps.user-agent:FORNITORI-project-work}") String userAgent,
            @Value("${fornitori.maps.viewbox:" + DEFAULT_VIEWBOX + "}") String viewbox) {
        if (!viewbox.isBlank() && !VIEWBOX.matcher(viewbox.trim()).matches()) {
            throw new IllegalArgumentException(
                    "fornitori.maps.viewbox deve essere lonMin,latMax,lonMax,latMin oppure vuoto");
        }
        this.viewbox = viewbox.trim();
        this.baseUrl = baseUrl;
        this.client = ProviderHttp.client(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    @Override
    public Optional<GeoPoint> geocode(String address) {
        waitForRateLimit();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/search")
                .queryParam("q", address)
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .queryParam("countrycodes", "it");
        if (!viewbox.isEmpty()) {
            builder.queryParam("viewbox", viewbox).queryParam("bounded", 1);
        }
        URI uri = builder
                .build()
                .encode()
                .toUri();
        try {
            Place[] places = client.get().uri(uri).retrieve().body(Place[].class);
            if (places == null || places.length == 0) {
                return Optional.empty();
            }
            return Optional.of(new GeoPoint(coordinate(places[0].lat()), coordinate(places[0].lon())));
        } catch (RestClientException | NumberFormatException | NullPointerException exception) {
            throw new ProviderUnavailableException("Geocodifica non disponibile");
        }
    }

    private synchronized void waitForRateLimit() {
        long waitMillis = lastRequestAt + MIN_INTERVAL_MILLIS - System.currentTimeMillis();
        if (waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ProviderUnavailableException("Geocodifica interrotta");
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }

    private static BigDecimal coordinate(String value) {
        return new BigDecimal(value).setScale(6, RoundingMode.HALF_UP);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Place(String lat, String lon) {
    }
}

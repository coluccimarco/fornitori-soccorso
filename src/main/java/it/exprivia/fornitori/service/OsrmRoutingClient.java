package it.exprivia.fornitori.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.exprivia.fornitori.service.MapProviders.GeoPoint;
import it.exprivia.fornitori.service.MapProviders.ProviderUnavailableException;
import it.exprivia.fornitori.service.MapProviders.RouteResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OsrmRoutingClient implements MapProviders.RoutingClient {
    private static final BigDecimal METERS_PER_KM = BigDecimal.valueOf(1000);

    private final String baseUrl;
    private final RestClient client;

    public OsrmRoutingClient(@Value("${fornitori.maps.osrm-url:https://router.project-osrm.org}") String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.client = ProviderHttp.client(baseUrl).build();
    }

    @Override
    public RouteResult route(List<GeoPoint> points) {
        String coordinates = points.stream()
                .map(point -> point.lng().toPlainString() + "," + point.lat().toPlainString())
                .collect(Collectors.joining(";"));
        URI uri = URI.create(baseUrl + "/route/v1/driving/" + coordinates + "?overview=full&geometries=geojson");
        try {
            Response response = client.get().uri(uri).retrieve().body(Response.class);
            if (response == null || !"Ok".equals(response.code())
                    || response.routes() == null || response.routes().isEmpty()) {
                throw new ProviderUnavailableException("Percorso non trovato");
            }
            Route route = response.routes().get(0);
            if (route.distance().signum() < 0) {
                throw new ProviderUnavailableException("Percorso non valido");
            }
            List<BigDecimal> legs = route.legs() == null ? List.of() : route.legs().stream()
                    .map(leg -> leg.distance().divide(METERS_PER_KM, 2, RoundingMode.HALF_UP))
                    .toList();
            return new RouteResult(route.distance().divide(METERS_PER_KM, 2, RoundingMode.HALF_UP), route.geometry(), legs);
        } catch (RestClientException | NullPointerException exception) {
            throw new ProviderUnavailableException("Calcolo percorso non disponibile");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Response(String code, List<Route> routes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Route(BigDecimal distance, Map<String, Object> geometry, List<Leg> legs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Leg(BigDecimal distance) {
    }
}

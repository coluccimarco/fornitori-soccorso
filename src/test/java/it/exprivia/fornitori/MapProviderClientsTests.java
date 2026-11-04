package it.exprivia.fornitori;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import it.exprivia.fornitori.service.MapProviders.GeoPoint;
import it.exprivia.fornitori.service.MapProviders.ProviderUnavailableException;
import it.exprivia.fornitori.service.MapProviders.RouteResult;
import it.exprivia.fornitori.service.NominatimGeocodingClient;
import it.exprivia.fornitori.service.OsrmRoutingClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MapProviderClientsTests {
    private HttpServer server;
    private String baseUrl;
    private int status = 200;
    private String body = "[]";
    private String requestUri;
    private String userAgent;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestUri = exchange.getRequestURI().toString();
            userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private NominatimGeocodingClient nominatim() {
        return new NominatimGeocodingClient(baseUrl, "agente-di-prova", "");
    }

    private NominatimGeocodingClient nominatimIn(String viewbox) {
        return new NominatimGeocodingClient(baseUrl, "agente-di-prova", viewbox);
    }

    @Test
    void geocodingStaysInsideTheConfiguredArea() {
        nominatimIn("15.8,42.2,18.6,39.7").geocode("Via Roma, Bitonto");
        assertTrue(requestUri.contains("viewbox=15.8,42.2,18.6,39.7"), requestUri);
        assertTrue(requestUri.contains("bounded=1"), requestUri);
    }

    @Test
    void geocodingIsNotLimitedWhenAreaIsEmpty() {
        nominatim().geocode("Via Roma, Bitonto");
        assertTrue(!requestUri.contains("viewbox") && !requestUri.contains("bounded"), requestUri);
    }

    @Test
    void invalidAreaIsRejectedAtStartup() {
        assertThrows(IllegalArgumentException.class, () -> nominatimIn("Puglia"));
        assertThrows(IllegalArgumentException.class, () -> nominatimIn("1,2,3"));
    }

    @Test
    void geocodingEncodesAddressAndIdentifiesItself() {
        body = "[{\"lat\":\"41.2006118\",\"lon\":\"16.5936656\",\"name\":\"Via Roma\"}]";
        Optional<GeoPoint> point = nominatim().geocode("Via Roma 10, Molfetta & co");
        assertEquals(new BigDecimal("41.200612"), point.orElseThrow().lat());
        assertEquals(new BigDecimal("16.593666"), point.orElseThrow().lng());
        assertEquals("agente-di-prova", userAgent);
        assertTrue(requestUri.startsWith("/search?q=Via%20Roma%2010,%20Molfetta%20%26%20co"), requestUri);
        assertTrue(requestUri.contains("countrycodes=it"), requestUri);
    }

    @Test
    void unknownAddressIsEmpty() {
        assertEquals(Optional.empty(), nominatim().geocode("Indirizzo inesistente"));
    }

    @Test
    void geocodingProviderErrorIsUnavailable() {
        status = 500;
        assertThrows(ProviderUnavailableException.class, () -> nominatim().geocode("Via Roma 10"));
    }

    @Test
    void malformedGeocodingResponseIsUnavailable() {
        body = "[{\"lat\":\"non-numero\",\"lon\":\"16\"}]";
        assertThrows(ProviderUnavailableException.class, () -> nominatim().geocode("Via Roma 10"));
    }

    @Test
    void routePassesPointsInOrderAndConvertsMetersToKm() {
        body = "{\"code\":\"Ok\",\"routes\":[{\"distance\":18456.7,"
                + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[16.5,41.2],[16.6,41.3]]}}]}";
        RouteResult route = new OsrmRoutingClient(baseUrl).route(List.of(
                new GeoPoint(new BigDecimal("41.2"), new BigDecimal("16.5")),
                new GeoPoint(new BigDecimal("41.3"), new BigDecimal("16.6"))));
        assertEquals(new BigDecimal("18.46"), route.km());
        assertEquals("LineString", route.geometry().get("type"));
        assertEquals("/route/v1/driving/16.5,41.2;16.6,41.3?overview=full&geometries=geojson", requestUri);
    }

    @Test
    void noRouteIsUnavailable() {
        body = "{\"code\":\"NoRoute\",\"routes\":[]}";
        OsrmRoutingClient client = new OsrmRoutingClient(baseUrl);
        assertThrows(ProviderUnavailableException.class, () -> client.route(List.of(
                new GeoPoint(BigDecimal.ONE, BigDecimal.ONE), new GeoPoint(BigDecimal.TWO, BigDecimal.TWO))));
    }

    @Test
    void routingProviderErrorIsUnavailable() {
        status = 503;
        body = "";
        OsrmRoutingClient client = new OsrmRoutingClient(baseUrl);
        assertThrows(ProviderUnavailableException.class, () -> client.route(List.of(
                new GeoPoint(BigDecimal.ONE, BigDecimal.ONE), new GeoPoint(BigDecimal.TWO, BigDecimal.TWO))));
    }
}

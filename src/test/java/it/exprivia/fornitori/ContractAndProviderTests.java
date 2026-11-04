package it.exprivia.fornitori;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import it.exprivia.fornitori.common.ApiError;
import it.exprivia.fornitori.controller.AssignmentController;
import it.exprivia.fornitori.controller.AuthController;
import it.exprivia.fornitori.controller.ChatController;
import it.exprivia.fornitori.controller.MapController;
import it.exprivia.fornitori.controller.MissionController;
import it.exprivia.fornitori.controller.RegistryController;
import it.exprivia.fornitori.dto.AuthDtos;
import it.exprivia.fornitori.dto.ChatDtos;
import it.exprivia.fornitori.dto.MapDtos;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.dto.RegistryDtos;
import it.exprivia.fornitori.service.MapProviders;
import it.exprivia.fornitori.service.NominatimGeocodingClient;
import it.exprivia.fornitori.service.OsrmRoutingClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.yaml.snakeyaml.Yaml;

/** Punto 19 e integrazioni esterne: non servono database né rete. */
class ContractAndProviderTests {
    private static final String API = "api/openapi.yaml";
    private static final String FRONTEND = "src/main/resources/static/app.js";
    private HttpServer server;
    private String base;

    @BeforeEach
    void startProvider() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopProvider() {
        server.stop(0);
    }

    private void respond(String path, int status, byte[] body, Map<String, String> headers) {
        server.createContext(path, exchange -> {
            headers.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> spec() throws IOException {
        try (var input = Files.newInputStream(Path.of(API))) {
            return new Yaml().load(input);
        }
    }

    private static List<MapProviders.GeoPoint> twoPoints() {
        return List.of(new MapProviders.GeoPoint(new BigDecimal("41.2"), new BigDecimal("16.6")),
                new MapProviders.GeoPoint(new BigDecimal("41.1"), new BigDecimal("16.8")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void openApiImplementedOperationsMatchTheControllers() throws Exception {
        Set<String> actual = new TreeSet<>();
        for (Class<?> controller : List.of(AuthController.class, RegistryController.class, MissionController.class,
                MapController.class, ChatController.class, AssignmentController.class)) {
            String root = controller.getAnnotation(RequestMapping.class).value()[0];
            for (var method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) actual.add("get " + root + method.getAnnotation(GetMapping.class).value()[0]);
                if (method.isAnnotationPresent(PostMapping.class)) actual.add("post " + root + method.getAnnotation(PostMapping.class).value()[0]);
                if (method.isAnnotationPresent(PatchMapping.class)) actual.add("patch " + root + method.getAnnotation(PatchMapping.class).value()[0]);
            }
        }
        Set<String> declared = new TreeSet<>();
        ((Map<String, Map<String, Object>>) spec().get("paths")).forEach((path, operations) ->
                operations.forEach((verb, value) -> {
                    if (value instanceof Map<?, ?> operation && "implemented".equals(operation.get("x-implementation-status"))) {
                        declared.add(verb + " " + path);
                    }
                }));
        assertThat(actual).isEqualTo(declared);
    }

    static Stream<Arguments> contractRecords() {
        return Stream.of(
                Arguments.of("ApiError", ApiError.class),
                Arguments.of("LoginRequest", AuthDtos.LoginRequest.class),
                Arguments.of("CurrentUser", AuthDtos.CurrentUser.class),
                Arguments.of("CsrfToken", AuthDtos.CsrfTokenResponse.class),
                Arguments.of("Customer", RegistryDtos.CustomerDto.class),
                Arguments.of("CreateCustomer", RegistryDtos.CreateCustomerRequest.class),
                Arguments.of("Driver", RegistryDtos.DriverDto.class),
                Arguments.of("Vehicle", RegistryDtos.VehicleDto.class),
                Arguments.of("Company", RegistryDtos.CompanyDto.class),
                Arguments.of("Workshop", RegistryDtos.WorkshopDto.class),
                Arguments.of("CreateMission", MissionDtos.CreateMissionRequest.class),
                Arguments.of("UpdateIntervention", MissionDtos.UpdateInterventionRequest.class),
                Arguments.of("ProposeMission", MissionDtos.ProposeMissionRequest.class),
                Arguments.of("Proposal", MissionDtos.ProposalResponse.class),
                Arguments.of("ProposalHistoryEntry", MissionDtos.ProposalHistoryEntry.class),
                Arguments.of("NearestProposal", MissionDtos.NearestProposalResponse.class),
                Arguments.of("CentreAlert", MissionDtos.CentreAlertResponse.class),
                Arguments.of("DriverMissionDetail", MissionDtos.DriverMissionDetailResponse.class),
                Arguments.of("CompleteMission", MissionDtos.CompleteMissionRequest.class),
                Arguments.of("CostAdjustment", MissionDtos.CostAdjustmentRequest.class),
                Arguments.of("CreateMissionNote", MissionDtos.CreateNoteRequest.class),
                Arguments.of("MissionNote", MissionDtos.MissionNoteResponse.class),
                Arguments.of("Mission", MissionDtos.MissionResponse.class),
                Arguments.of("Dashboard", MissionDtos.DashboardResponse.class),
                Arguments.of("DriverTotal", MissionDtos.DriverTotal.class),
                Arguments.of("DriverHistory", MissionDtos.DriverHistoryResponse.class),
                Arguments.of("ChatMissionRequest", ChatDtos.CreateMissionRequest.class),
                Arguments.of("ChatMissionCreated", ChatDtos.MissionCreatedResponse.class),
                Arguments.of("ChatVehicleLookup", ChatDtos.VehicleLookupRequest.class),
                Arguments.of("ChatVehicleLookupResult", ChatDtos.VehicleLookupResponse.class),
                Arguments.of("ChatStatusRequest", ChatDtos.MissionStatusRequest.class),
                Arguments.of("MissionStatus", ChatDtos.MissionStatusResponse.class),
                Arguments.of("ChatNotifications", ChatDtos.NotificationsResponse.class),
                Arguments.of("ChatNotification", ChatDtos.NotificationResponse.class),
                Arguments.of("ChatNotificationClaim", ChatDtos.NotificationClaimRequest.class),
                Arguments.of("ChatNotificationResult", ChatDtos.NotificationResultRequest.class),
                Arguments.of("Route", MapDtos.RouteResponse.class));
    }

    @ParameterizedTest
    @MethodSource("contractRecords")
    @SuppressWarnings("unchecked")
    void dtoFieldsMatchOpenApi(String schema, Class<?> dto) throws Exception {
        var components = (Map<String, Object>) spec().get("components");
        var schemas = (Map<String, Map<String, Object>>) components.get("schemas");
        var properties = (Map<String, Object>) schemas.get(schema).get("properties");
        Set<String> fields = new HashSet<>();
        for (var field : dto.getRecordComponents()) {
            fields.add(field.getName());
        }
        assertThat(properties.keySet()).as(schema).isEqualTo(fields);
    }

    @Test
    void everyFrontendCallExistsInTheContract() throws Exception {
        String source = Files.readString(Path.of(FRONTEND));
        var matcher = Pattern.compile("/api/v1/(?:[A-Za-z0-9_/.\\-]|\\$\\{[^}]*\\})*").matcher(source);
        List<String> calls = new ArrayList<>();
        while (matcher.find()) {
            calls.add(matcher.group());
        }
        @SuppressWarnings("unchecked")
        var paths = ((Map<String, Object>) spec().get("paths")).keySet();
        assertThat(calls).hasSizeGreaterThan(15).allSatisfy(call -> {
            String regex = call.replaceAll("\\$\\{[^}]*\\}", "[^/]*").replace("/api/v1", "/api/v1");
            assertThat(paths.stream().anyMatch(path -> path.replaceAll("\\{[^}]+\\}", "x").matches(regex))).as(call).isTrue();
        });
    }

    @Test
    void frontendEscapesServerData() throws Exception {
        String source = Files.readString(Path.of(FRONTEND));
        assertThat(source).contains("replaceAll(\"<\", \"&lt;\")", "replaceAll('\"', \"&quot;\")");
    }

    @Test
    void providerConfigurationRejectsUnsafeUrls() {
        for (String url : List.of("ftp://example.org", "file:///etc/passwd", "http://user:secret@example.org")) {
            assertThatThrownBy(() -> new OsrmRoutingClient(url)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void osrmValidResponseIsParsed() {
        respond("/route", 200, ("{\"code\":\"Ok\",\"routes\":[{\"distance\":18400,\"geometry\":"
                + "{\"type\":\"LineString\",\"coordinates\":[]}}]}").getBytes(StandardCharsets.UTF_8), Map.of("Content-Type", "application/json"));
        assertThat(new OsrmRoutingClient(base).route(twoPoints()).km()).isEqualByComparingTo("18.40");
    }

    @Test
    void osrmRedirectIsNotFollowed() {
        respond("/route", 302, new byte[0], Map.of("Location", "http://169.254.169.254/latest/meta-data"));
        assertThatThrownBy(() -> new OsrmRoutingClient(base).route(twoPoints()))
                .isInstanceOf(MapProviders.ProviderUnavailableException.class);
    }

    @Test
    void oversizedProviderResponseIsRejected() {
        respond("/route", 200, new byte[1_048_577], Map.of());
        assertThatThrownBy(() -> new OsrmRoutingClient(base).route(twoPoints()))
                .isInstanceOf(MapProviders.ProviderUnavailableException.class);
    }

    @Test
    void invalidProviderValuesAreRejected() {
        respond("/route", 200, "{\"code\":\"Ok\",\"routes\":[{\"distance\":-1,\"geometry\":{}}]}".getBytes(StandardCharsets.UTF_8), Map.of("Content-Type", "application/json"));
        assertThatThrownBy(() -> new OsrmRoutingClient(base).route(twoPoints()))
                .isInstanceOf(MapProviders.ProviderUnavailableException.class);
    }

    @Test
    void providerErrorIsReportedAsUnavailable() {
        respond("/route", 500, new byte[0], Map.of());
        assertThatThrownBy(() -> new OsrmRoutingClient(base).route(twoPoints()))
                .isInstanceOf(MapProviders.ProviderUnavailableException.class);
    }

    @Test
    void nominatimRejectsOutOfRangeCoordinatesAndRespectsTheRate() {
        respond("/search", 200, "[{\"lat\":\"120\",\"lon\":\"16.6\"}]".getBytes(StandardCharsets.UTF_8), Map.of("Content-Type", "application/json"));
        var client = new NominatimGeocodingClient(base, "agente-di-prova", "");
        assertThatThrownBy(() -> client.geocode("Via Roma")).isInstanceOf(MapProviders.ProviderUnavailableException.class);
        long start = System.nanoTime();
        assertThatThrownBy(() -> client.geocode("Via Roma")).isInstanceOf(MapProviders.ProviderUnavailableException.class);
        assertThat((System.nanoTime() - start) / 1_000_000).isGreaterThanOrEqualTo(900);
    }
}

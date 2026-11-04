package it.exprivia.fornitori;

import it.exprivia.fornitori.dto.*;
import it.exprivia.fornitori.common.ApiError;
import it.exprivia.fornitori.controller.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.Yaml;
import static org.junit.jupiter.api.Assertions.*;

class OpenApiContractTests {
    @SuppressWarnings("unchecked")
    private static Map<String, Object> spec() throws Exception {
        try (var input = Files.newInputStream(Path.of("api/openapi.yaml"))) { return new Yaml().load(input); }
    }
    static Stream<Arguments> records() {
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
            Arguments.of("ChatNotificationResult", ChatDtos.NotificationResultRequest.class),
            Arguments.of("Route", MapDtos.RouteResponse.class));
    }

    @ParameterizedTest @MethodSource("records")
    @SuppressWarnings("unchecked")
    void dtoFieldsAndTypesMatchOpenApi(String name, Class<?> dto) throws Exception {
        var components = (Map<String, Object>) spec().get("components");
        var schemas = (Map<String, Map<String, Object>>) components.get("schemas");
        var properties = (Map<String, Map<String, Object>>) schemas.get(name).get("properties");
        var fields = new HashSet<String>();
        for (var field : dto.getRecordComponents()) {
            fields.add(field.getName());
            String type = field.getType() == String.class || field.getType().getPackageName().equals("java.time") ? "string"
                    : field.getType() == Long.class || field.getType() == long.class || field.getType() == int.class ? "integer"
                    : field.getType() == Boolean.class || field.getType() == boolean.class ? "boolean"
                    : field.getType() == java.math.BigDecimal.class ? "number"
                    : List.class.isAssignableFrom(field.getType()) ? "array" : "object";
            assertNotNull(properties.get(field.getName()), name + "." + field.getName());
            assertEquals(type, properties.get(field.getName()).get("type"), name + "." + field.getName());
        }
        assertEquals(fields, properties.keySet(), name);
    }

    @Test @SuppressWarnings("unchecked")
    void everyImplementedOperationHasControllerAndViceVersa() throws Exception {
        Set<String> actual = new TreeSet<>();
        for (Class<?> controller : List.of(AuthController.class, RegistryController.class,
                MissionController.class, MapController.class, ChatController.class, AssignmentController.class)) {
            String base = controller.getAnnotation(RequestMapping.class).value()[0];
            for (var method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) actual.add("get " + base + method.getAnnotation(GetMapping.class).value()[0]);
                if (method.isAnnotationPresent(PostMapping.class)) actual.add("post " + base + method.getAnnotation(PostMapping.class).value()[0]);
                if (method.isAnnotationPresent(PatchMapping.class)) actual.add("patch " + base + method.getAnnotation(PatchMapping.class).value()[0]);
            }
        }
        Set<String> declared = new TreeSet<>();
        var paths = (Map<String, Map<String, Object>>) spec().get("paths");
        paths.forEach((path, operations) -> operations.forEach((verb, value) -> {
            if (value instanceof Map<?, ?> operation && "implemented".equals(operation.get("x-implementation-status")))
                declared.add(verb + " " + path);
        }));
        assertEquals(declared, actual);
    }
}

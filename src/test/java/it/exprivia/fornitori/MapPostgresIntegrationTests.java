package it.exprivia.fornitori;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.exprivia.fornitori.common.ForbiddenOperationException;
import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.dto.MapDtos;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.repository.MissionRepository;
import it.exprivia.fornitori.service.MapProviders;
import it.exprivia.fornitori.service.MapProviders.GeoPoint;
import it.exprivia.fornitori.service.MapProviders.ProviderUnavailableException;
import it.exprivia.fornitori.service.MapProviders.RouteResult;
import it.exprivia.fornitori.service.MapService;
import it.exprivia.fornitori.service.MissionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_PASSWORD", matches = ".+")
class MapPostgresIntegrationTests {
    private static final String RESPONSABILE = "responsabile@demo.it";
    private static final String AUTISTA_1 = "autista1@demo.it";
    private static final String AUTISTA_2 = "autista2@demo.it";
    private static final GeoPoint GEOCODED = new GeoPoint(new BigDecimal("41.200612"), new BigDecimal("16.593666"));
    private static final BigDecimal ROUTE_KM = new BigDecimal("18.46");

    @MockitoBean
    private MapProviders.GeocodingClient geocoding;

    @MockitoBean
    private MapProviders.RoutingClient routing;

    @Autowired
    private MapService maps;

    @Autowired
    private MissionService missions;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void stubProviders() {
        when(geocoding.geocode(any())).thenReturn(Optional.of(GEOCODED));
        when(routing.route(anyList())).thenReturn(new RouteResult(ROUTE_KM, Map.of("type", "LineString")));
    }

    private MissionDtos.MissionResponse repair(BigDecimal lat, BigDecimal lng) {
        return missions.create(RESPONSABILE, new MissionDtos.CreateMissionRequest(
                1L, "riparazione_sul_posto", "Motore in panne", "Via Roma 10, Molfetta",
                lat, lng, null, null, "centrale"));
    }

    private MissionDtos.MissionResponse towing() {
        return missions.create(RESPONSABILE, new MissionDtos.CreateMissionRequest(
                1L, "traino", "Motore in panne", "Via Roma 10, Molfetta",
                GEOCODED.lat(), GEOCODED.lng(), 1L, null, "centrale"));
    }

    private BigDecimal savedKm(Long missionId) {
        return missionRepository.findById(missionId).orElseThrow().getKmStimati();
    }

    private static HttpStatus statusOf(ResponseStatusException exception) {
        return HttpStatus.valueOf(exception.getStatusCode().value());
    }

    @Test
    void addressIsGeocodedOnceAndSaved() {
        MissionDtos.MissionResponse mission = repair(null, null);

        MapDtos.RouteResponse first = maps.route(RESPONSABILE, mission.id(), 1L);
        maps.route(RESPONSABILE, mission.id(), 1L);

        assertEquals(GEOCODED.lat(), first.punti().get(1).lat());
        verify(geocoding, times(1)).geocode("Via Roma 10, Molfetta");
        assertEquals(0, GEOCODED.lat().compareTo(missionRepository.findById(mission.id()).orElseThrow().getOrigineLat()));
    }

    @Test
    void sharedPositionIsNotGeocoded() {
        MissionDtos.MissionResponse mission = repair(new BigDecimal("41.2"), new BigDecimal("16.6"));
        maps.route(RESPONSABILE, mission.id(), 1L);
        verify(geocoding, never()).geocode(any());
    }

    @Test
    void repairGoesFromBaseToAccident() {
        MapDtos.RouteResponse route = maps.route(RESPONSABILE, repair(null, null).id(), 1L);
        assertEquals(List.of("base", "sinistro"), route.punti().stream().map(MapDtos.RoutePoint::tipo).toList());
        assertEquals(ROUTE_KM, route.kmStimati());
        assertEquals("osrm", route.origine());
    }

    @Test
    void towingContinuesToWorkshop() {
        MapDtos.RouteResponse route = maps.route(RESPONSABILE, towing().id(), 1L);
        assertEquals(List.of("base", "sinistro", "officina"),
                route.punti().stream().map(MapDtos.RoutePoint::tipo).toList());
    }

    @Test
    void estimatedKmAreKeptForAssignedDriverOnly() {
        MissionDtos.MissionResponse mission = repair(null, null);
        maps.route(RESPONSABILE, mission.id(), 2L);
        assertEquals(0, ROUTE_KM.compareTo(savedKm(mission.id())));

        missions.propose(RESPONSABILE, mission.id(), new MissionDtos.ProposeMissionRequest(1L));
        when(routing.route(anyList())).thenReturn(new RouteResult(new BigDecimal("99.00"), Map.of()));
        maps.route(RESPONSABILE, mission.id(), 2L);
        org.junit.jupiter.api.Assertions.assertNull(savedKm(mission.id()));

        maps.route(AUTISTA_1, mission.id(), null);
        assertEquals(0, new BigDecimal("99.00").compareTo(savedKm(mission.id())));
    }

    @Test
    void completedMissionKeepsItsEstimate() {
        MissionDtos.MissionResponse mission = repair(null, null);
        missions.propose(RESPONSABILE, mission.id(), new MissionDtos.ProposeMissionRequest(1L));
        maps.route(AUTISTA_1, mission.id(), null);
        missions.accept(AUTISTA_1, mission.id());
        missions.complete(AUTISTA_1, mission.id(), new MissionDtos.CompleteMissionRequest(new BigDecimal("20.00")));

        when(routing.route(anyList())).thenReturn(new RouteResult(new BigDecimal("99.00"), Map.of()));
        maps.route(RESPONSABILE, mission.id(), 1L);
        assertEquals(0, ROUTE_KM.compareTo(savedKm(mission.id())));
    }

    @Test
    void driverCannotUseAnotherDriverOrMission() {
        MissionDtos.MissionResponse mission = repair(null, null);
        missions.propose(RESPONSABILE, mission.id(), new MissionDtos.ProposeMissionRequest(1L));

        assertThrows(ForbiddenOperationException.class, () -> maps.route(AUTISTA_1, mission.id(), 2L));
        assertThrows(ForbiddenOperationException.class, () -> maps.route(AUTISTA_2, mission.id(), null));
        assertEquals(1L, maps.route(AUTISTA_1, mission.id(), null).autistaId());
    }

    @Test
    void withoutDriverCentreSeesOnlyWhereCustomerIs() {
        MissionDtos.MissionResponse mission = repair(null, null);

        MapDtos.RouteResponse route = maps.route(RESPONSABILE, mission.id(), null);

        assertEquals(List.of("sinistro"), route.punti().stream().map(MapDtos.RoutePoint::tipo).toList());
        assertEquals(GEOCODED.lat(), route.punti().get(0).lat());
        assertNull(route.kmStimati());
        assertNull(route.geometria());
        assertNull(savedKm(mission.id()));
        verify(routing, never()).route(anyList());
    }

    @Test
    void driverSeesCustomerWorkshopAndEstimateOnOpenMissionOnly() {
        MissionDtos.MissionResponse mission = towing();
        missions.propose(RESPONSABILE, mission.id(), new MissionDtos.ProposeMissionRequest(1L));

        MissionDtos.DriverMissionDetailResponse beforeRoute = missions.driverMissionDetail(AUTISTA_1, mission.id());
        assertNull(beforeRoute.cliente().nome());
        assertNull(beforeRoute.cliente().telefono());
        assertNull(beforeRoute.cliente().targa());
        assertEquals("Audi A3", beforeRoute.cliente().modelloVeicolo());
        assertNotNull(beforeRoute.officina().nome());
        assertNull(beforeRoute.guadagnoStimato());

        maps.route(AUTISTA_1, mission.id(), null);
        missionRepository.flush();
        MissionDtos.DriverMissionDetailResponse afterRoute = missions.driverMissionDetail(AUTISTA_1, mission.id());
        BigDecimal day = new BigDecimal("72.15");
        BigDecimal night = new BigDecimal("90.19");
        assertTrue(afterRoute.guadagnoStimato().compareTo(day) == 0 || afterRoute.guadagnoStimato().compareTo(night) == 0,
                "stima: " + afterRoute.guadagnoStimato());

        missions.accept(AUTISTA_1, mission.id());
        MissionDtos.DriverMissionDetailResponse accepted = missions.driverMissionDetail(AUTISTA_1, mission.id());
        assertEquals("accettata", accepted.missione().stato());
        assertEquals("Lucia Gallo", accepted.cliente().nome());
        assertEquals("PW 660 SW", accepted.cliente().targa());
        assertNotNull(accepted.cliente().telefono());

        assertThrows(ForbiddenOperationException.class, () -> missions.driverMissionDetail(AUTISTA_2, mission.id()));
        assertThrows(ForbiddenOperationException.class, () -> missions.driverMissionDetail(RESPONSABILE, mission.id()));

        missions.complete(AUTISTA_1, mission.id(), new MissionDtos.CompleteMissionRequest(new BigDecimal("20.00")));
        assertThrows(MissionConflictException.class, () -> missions.driverMissionDetail(AUTISTA_1, mission.id()));
    }

    @Test
    void rejectingDriverNeverReceivesCustomerContacts() {
        MissionDtos.MissionResponse mission = repair(null, null);
        missions.propose(RESPONSABILE, mission.id(), new MissionDtos.ProposeMissionRequest(1L));
        MissionDtos.DriverCustomerDetail seen = missions.driverMissionDetail(AUTISTA_1, mission.id()).cliente();
        assertNull(seen.nome());
        assertNull(seen.telefono());

        missions.reject(AUTISTA_1, mission.id());

        assertThrows(ForbiddenOperationException.class, () -> missions.driverMissionDetail(AUTISTA_1, mission.id()));
    }

    @Test
    void driverMissionListAndGetHideContactsUntilAcceptance() {
        MissionDtos.MissionResponse mission = repair(null, null);
        missions.propose(RESPONSABILE, mission.id(), new MissionDtos.ProposeMissionRequest(1L));

        MissionDtos.MissionResponse listed = missions.driverMissions(AUTISTA_1).get(0);
        MissionDtos.MissionResponse fetched = missions.get(AUTISTA_1, mission.id());
        for (MissionDtos.MissionResponse seen : List.of(listed, fetched)) {
            assertNull(seen.clienteNome());
            assertNull(seen.clienteTelefono());
        }
        assertNotNull(missions.get(RESPONSABILE, mission.id()).clienteNome());

        missions.accept(AUTISTA_1, mission.id());
        assertEquals("Lucia Gallo", missions.driverMissions(AUTISTA_1).get(0).clienteNome());
        assertNotNull(missions.get(AUTISTA_1, mission.id()).clienteTelefono());
    }

    @Test
    void driverCannotReadUnassignedMission() {
        MissionDtos.MissionResponse mission = repair(null, null);
        assertThrows(ForbiddenOperationException.class, () -> missions.driverMissionDetail(AUTISTA_1, mission.id()));
    }

    @Test
    void unknownAddressIsReported() {
        when(geocoding.geocode(any())).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> maps.route(RESPONSABILE, repair(null, null).id(), 1L));
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, statusOf(error));
    }

    @Test
    void providerOutageDoesNotInventKmAndKeepsGeocoding() {
        MissionDtos.MissionResponse mission = repair(null, null);
        when(routing.route(anyList())).thenThrow(new ProviderUnavailableException("giu"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> maps.route(RESPONSABILE, mission.id(), 1L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, statusOf(error));
        assertNull(savedKm(mission.id()));
        assertEquals(0, GEOCODED.lat().compareTo(missionRepository.findById(mission.id()).orElseThrow().getOrigineLat()));
    }

    @Test
    void geocodingOutageIsUnavailable() {
        when(geocoding.geocode(any())).thenThrow(new ProviderUnavailableException("giu"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> maps.route(RESPONSABILE, repair(null, null).id(), 1L));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, statusOf(error));
    }

    @Test
    void historicalMissionsAreNotChanged() {
        Map<String, Object> fixture = jdbcTemplate.queryForMap(
                "SELECT id, km_stimati FROM missions WHERE is_fixture ORDER BY id LIMIT 1");
        Long id = ((Number) fixture.get("id")).longValue();
        maps.route(RESPONSABILE, id, 1L);
        assertEquals(fixture.get("km_stimati"), jdbcTemplate.queryForMap(
                "SELECT km_stimati FROM missions WHERE id = ?", id).get("km_stimati"));
    }

    @Test
    void slowRouteCannotPersistEstimateForChangedIntervention() {
        MissionDtos.MissionResponse mission = repair(null, null);
        when(routing.route(anyList())).thenAnswer(invocation -> {
            missions.updateIntervention(RESPONSABILE, mission.id(), new MissionDtos.UpdateInterventionRequest("traino", 1L));
            return new RouteResult(ROUTE_KM, Map.of("type", "LineString"));
        });
        maps.route(RESPONSABILE, mission.id(), 1L);
        assertNull(savedKm(mission.id()));
        assertEquals("traino", missions.get(RESPONSABILE, mission.id()).tipoIntervento());
    }
}

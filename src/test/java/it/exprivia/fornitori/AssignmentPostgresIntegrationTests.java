package it.exprivia.fornitori;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.exprivia.fornitori.common.ForbiddenOperationException;
import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.repository.MissionRepository;
import it.exprivia.fornitori.service.CentreAlertService;
import it.exprivia.fornitori.service.MapProviders;
import it.exprivia.fornitori.service.MapProviders.GeoPoint;
import it.exprivia.fornitori.service.MissionService;
import it.exprivia.fornitori.service.NearestDriverService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_PASSWORD", matches = ".+")
class AssignmentPostgresIntegrationTests {
    private static final String RESPONSABILE = "responsabile@demo.it";
    // Basi dei dati del docente: Bitonto è l'autista 4, poi Giovinazzo (3), Bari (8) e Ruvo (6).
    private static final BigDecimal BITONTO_LAT = new BigDecimal("41.108300");
    private static final BigDecimal BITONTO_LNG = new BigDecimal("16.691400");

    @MockitoBean
    private MapProviders.GeocodingClient geocoding;

    @MockitoBean
    private MapProviders.RoutingClient routing;

    @Autowired
    private NearestDriverService nearest;

    @Autowired
    private CentreAlertService alerts;

    @Autowired
    private MissionService missions;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private MissionDtos.MissionResponse missionNearBitonto() {
        return missions.create(RESPONSABILE, new MissionDtos.CreateMissionRequest(
                1L, "riparazione_sul_posto", "Prova", "Via Roma 1, Bitonto",
                BITONTO_LAT, BITONTO_LNG, null, null, "centrale"));
    }

    @Test
    void proposesToFreeDriverWithNearestBase() {
        MissionDtos.NearestProposalResponse result = nearest.proposeToNearest(RESPONSABILE, missionNearBitonto().id());

        assertEquals(4L, result.autistaId());
        assertEquals("Francesco Amoruso", result.autistaNome());
        assertEquals("proposta", result.stato());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.distanzaKm()));
        verify(geocoding, never()).geocode(any());
    }

    @Test
    void skipsBusyDriversAndPicksTheNextNearest() {
        nearest.proposeToNearest(RESPONSABILE, missionNearBitonto().id());

        MissionDtos.NearestProposalResponse second = nearest.proposeToNearest(RESPONSABILE, missionNearBitonto().id());

        assertEquals(3L, second.autistaId());
        assertTrue(second.distanzaKm().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void doesNotProposeAgainToDriverWhoRejected() {
        MissionDtos.MissionResponse mission = missionNearBitonto();
        assertEquals(4L, nearest.proposeToNearest(RESPONSABILE, mission.id()).autistaId());
        missions.reject("autista4@demo.it", mission.id());

        MissionDtos.NearestProposalResponse retry = nearest.proposeToNearest(RESPONSABILE, mission.id());

        assertEquals(3L, retry.autistaId());
    }

    @Test
    void failsWhenNoDriverIsFree() {
        for (long driverId = 1; driverId <= 8; driverId++) {
            missions.propose(RESPONSABILE, missionNearBitonto().id(), new MissionDtos.ProposeMissionRequest(driverId));
        }
        MissionDtos.MissionResponse mission = missionNearBitonto();

        assertThrows(MissionConflictException.class, () -> nearest.proposeToNearest(RESPONSABILE, mission.id()));
        assertEquals("nuova", missionRepository.findById(mission.id()).orElseThrow().getStato());
    }

    @Test
    void geocodesAddressOnceWhenCoordinatesAreMissing() {
        when(geocoding.geocode("Via Roma 1, Bitonto")).thenReturn(Optional.of(new GeoPoint(BITONTO_LAT, BITONTO_LNG)));
        MissionDtos.MissionResponse mission = missions.create(RESPONSABILE, new MissionDtos.CreateMissionRequest(
                1L, "riparazione_sul_posto", "Prova", "Via Roma 1, Bitonto", null, null, null, null, "telegram"));

        assertEquals(4L, nearest.proposeToNearest(RESPONSABILE, mission.id()).autistaId());

        assertEquals(0, BITONTO_LAT.compareTo(missionRepository.findById(mission.id()).orElseThrow().getOrigineLat()));
    }

    @Test
    void unknownAddressIsReportedWithoutProposal() {
        when(geocoding.geocode(any())).thenReturn(Optional.empty());
        MissionDtos.MissionResponse mission = missions.create(RESPONSABILE, new MissionDtos.CreateMissionRequest(
                1L, "riparazione_sul_posto", "Prova", "Via Inesistente 99", null, null, null, null, "telegram"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> nearest.proposeToNearest(RESPONSABILE, mission.id()));

        assertEquals(422, error.getStatusCode().value());
        assertEquals("nuova", missionRepository.findById(mission.id()).orElseThrow().getStato());
    }

    @Test
    void onlyTheCentreCanUseIt() {
        MissionDtos.MissionResponse mission = missionNearBitonto();
        assertThrows(ForbiddenOperationException.class, () -> nearest.proposeToNearest("autista1@demo.it", mission.id()));
        assertThrows(ForbiddenOperationException.class, () -> alerts.latest("autista1@demo.it"));
    }

    @Test
    void missionAlreadyProposedOrHistoricalIsRefused() {
        MissionDtos.MissionResponse mission = missionNearBitonto();
        nearest.proposeToNearest(RESPONSABILE, mission.id());
        assertThrows(MissionConflictException.class, () -> nearest.proposeToNearest(RESPONSABILE, mission.id()));

        Long fixtureId = jdbcTemplate.queryForObject("SELECT id FROM missions WHERE is_fixture LIMIT 1", Long.class);
        assertThrows(RuntimeException.class, () -> nearest.proposeToNearest(RESPONSABILE, fixtureId));
    }

    @Test
    void rejectionAndExpiryProduceAlertsWithDriverNames() {
        MissionDtos.MissionResponse mission = missionNearBitonto();
        assertTrue(alerts.latest(RESPONSABILE).isEmpty());

        nearest.proposeToNearest(RESPONSABILE, mission.id());
        missions.reject("autista4@demo.it", mission.id());
        nearest.proposeToNearest(RESPONSABILE, mission.id());
        entityManager.flush();
        jdbcTemplate.update("UPDATE missions SET scadenza_il = now() - interval '1 minute' WHERE id = ?", mission.id());
        entityManager.clear();
        assertEquals(1, missions.expireDueProposals());

        List<MissionDtos.CentreAlertResponse> list = alerts.latest(RESPONSABILE);

        assertEquals(2, list.size());
        MissionDtos.CentreAlertResponse expired = list.get(0);
        assertEquals("scaduta", expired.tipo());
        assertEquals(3L, expired.autistaId());
        assertEquals("Antonio Lorusso", expired.autistaNome());
        assertEquals(mission.id(), expired.missioneId());
        assertEquals(mission.codice(), expired.codice());
        MissionDtos.CentreAlertResponse rejected = list.get(1);
        assertEquals("rifiutata", rejected.tipo());
        assertEquals("Francesco Amoruso", rejected.autistaNome());
        assertTrue(expired.id() > rejected.id());
    }

    @Test
    void alertsIgnoreHistoricalMissionsAndOtherEvents() {
        MissionDtos.MissionResponse mission = missionNearBitonto();
        nearest.proposeToNearest(RESPONSABILE, mission.id());
        missions.accept("autista4@demo.it", mission.id());

        assertTrue(alerts.latest(RESPONSABILE).isEmpty());
        assertNull(alerts.latest(RESPONSABILE).stream().findAny().orElse(null));
    }
}

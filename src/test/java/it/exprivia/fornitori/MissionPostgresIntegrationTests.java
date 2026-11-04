package it.exprivia.fornitori;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.common.ForbiddenOperationException;
import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.repository.MissionEventRepository;
import it.exprivia.fornitori.repository.MissionNoteRepository;
import it.exprivia.fornitori.repository.MissionProposalRepository;
import it.exprivia.fornitori.repository.MissionRepository;
import it.exprivia.fornitori.repository.NotificationOutboxRepository;
import it.exprivia.fornitori.service.MissionService;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_PASSWORD", matches = ".+")
class MissionPostgresIntegrationTests {
    @Autowired
    private MissionService missions;

    @Autowired
    private NotificationOutboxRepository outbox;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private MissionProposalRepository proposalRepository;

    @Autowired
    private MissionEventRepository eventRepository;

    @Autowired
    private MissionNoteRepository noteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional
    void createsProposesAcceptsAndCompletesLiveMissionWithoutChangingFixtures() {
        long fixtureCount = missionRepository.countByFixtureTrue();
        MissionDtos.MissionResponse created = missions.create(
                "responsabile@demo.it",
                new MissionDtos.CreateMissionRequest(
                        1L, "riparazione_sul_posto", "Prova integrazione",
                        "Via Roma 1, Molfetta", new BigDecimal("41.200000"),
                        new BigDecimal("16.590000"), null, new BigDecimal("18.00"), "centrale"));
        assertEquals("nuova", created.stato());

        MissionDtos.ProposalResponse proposal =
                missions.propose("responsabile@demo.it", created.id(), new MissionDtos.ProposeMissionRequest(1L));
        assertEquals("proposta", proposal.stato());
        assertNotNull(proposal.scadenzaIl());
        assertEquals(created.id(), missions.addNote(
                "autista1@demo.it", created.id(), new MissionDtos.CreateNoteRequest("In viaggio")).missionId());

        assertThrows(ForbiddenOperationException.class, () -> missions.accept("autista2@demo.it", created.id()));
        MissionDtos.MissionResponse accepted = missions.accept("autista1@demo.it", created.id());
        assertEquals("accettata", accepted.stato());

        MissionDtos.MissionResponse completed = missions.complete(
                "autista1@demo.it", created.id(),
                new MissionDtos.CompleteMissionRequest(new BigDecimal("18.00")));
        assertEquals("eseguita", completed.stato());
        assertNotNull(completed.costoCalcolato());
        assertEquals(completed.costoCalcolato(), completed.costoFinale());
        assertEquals("MISSION_ACCEPTED:" + created.id(),
                outbox.findByEventKey("MISSION_ACCEPTED:" + created.id()).orElseThrow().getEventKey());
        assertEquals("MISSION_COMPLETED:" + created.id(),
                outbox.findByEventKey("MISSION_COMPLETED:" + created.id()).orElseThrow().getEventKey());
        BigDecimal calculatedCost = completed.costoCalcolato();
        MissionDtos.MissionResponse adjusted = missions.adjustCost(
                "responsabile@demo.it", created.id(),
                new MissionDtos.CostAdjustmentRequest(new BigDecimal("42.50"), "Correzione concordata"));
        assertEquals(calculatedCost, adjusted.costoCalcolato());
        assertEquals(new BigDecimal("42.50"), adjusted.costoFinale());
        assertEquals("Correzione concordata", adjusted.rettificaMotivo());
        MissionDtos.DriverHistoryResponse history = missions.driverHistory("autista1@demo.it");
        assertEquals(new BigDecimal("42.50"), history.totaleEur());
        MissionDtos.DashboardResponse dashboard = missions.dashboard("responsabile@demo.it", "live");
        assertEquals(1, dashboard.eseguite());
        assertEquals(new BigDecimal("42.50"), dashboard.totaleEur());
        assertEquals(1, dashboard.totalePerAutista().size());
        assertEquals(new BigDecimal("42.50"), dashboard.totalePerAutista().getFirst().totaleEur());
        assertThrows(MissionConflictException.class, () -> missions.complete(
                "autista1@demo.it", created.id(),
                new MissionDtos.CompleteMissionRequest(new BigDecimal("18.00"))));
        assertEquals(fixtureCount, missionRepository.countByFixtureTrue());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentProposalsCannotAssignOneDriverToTwoMissions() throws Exception {
        MissionDtos.MissionResponse first = createMission();
        MissionDtos.MissionResponse second = createMission();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> firstResult = executor.submit(() -> tryPropose(first.id(), ready, start));
            Future<Boolean> secondResult = executor.submit(() -> tryPropose(second.id(), ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(firstResult.get(20, TimeUnit.SECONDS), secondResult.get(20, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            deleteMission(first.id());
            deleteMission(second.id());
        }
    }

    @Test
    @Transactional
    void rejectedProposalCanBeReissuedAndExpiredBySchedulerWithoutLosingHistory() {
        MissionDtos.MissionResponse created = createMission();
        missions.propose("responsabile@demo.it", created.id(), new MissionDtos.ProposeMissionRequest(1L));
        MissionDtos.MissionResponse rejected = missions.reject("autista1@demo.it", created.id());
        assertEquals("non_accettata", rejected.stato());

        missions.propose("responsabile@demo.it", created.id(), new MissionDtos.ProposeMissionRequest(2L));
        entityManager.flush();
        jdbcTemplate.update(
                "UPDATE missions SET scadenza_il = now() - interval '1 minute' WHERE id = ?",
                created.id());
        entityManager.clear();

        assertEquals(1, missions.expireDueProposals());
        assertEquals("scaduta", missionRepository.findById(created.id()).orElseThrow().getStato());
        var history = proposalRepository.findByMissionIdAndFixtureFalseOrderByIdAsc(created.id());
        assertEquals(2, history.size());
        assertEquals("non_accettata", history.get(0).getStato());
        assertEquals("scaduta", history.get(1).getStato());
    }

    private MissionDtos.MissionResponse createMission() {
        return missions.create(
                "responsabile@demo.it",
                new MissionDtos.CreateMissionRequest(
                        1L, "riparazione_sul_posto", "Prova concorrenza",
                        "Via Roma 1, Molfetta", new BigDecimal("41.200000"),
                        new BigDecimal("16.590000"), null, new BigDecimal("18.00"), "centrale"));
    }

    @Test
    void concurrentAcceptanceCommitsOnce() throws Exception {
        MissionDtos.MissionResponse mission = createMission();
        missions.propose("responsabile@demo.it", mission.id(), new MissionDtos.ProposeMissionRequest(1L));
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        java.util.concurrent.Callable<Boolean> attempt = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("No start");
            try { missions.accept("autista1@demo.it", mission.id()); return true; }
            catch (MissionConflictException expected) { return false; }
        };
        try {
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1, jdbcTemplate.queryForObject(
                    "select count(*) from notifications_outbox where mission_id=? and event_key=?", Integer.class,
                    mission.id(), "MISSION_ACCEPTED:" + mission.id()));
            assertEquals("accettata", jdbcTemplate.queryForObject("select stato from missions where id=?", String.class, mission.id()));
        } finally {
            start.countDown(); executor.shutdownNow(); executor.awaitTermination(10, TimeUnit.SECONDS);
            deleteMission(mission.id());
        }
    }

    private boolean tryPropose(long missionId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent proposal test did not start");
        }
        try {
            missions.propose(
                    "responsabile@demo.it", missionId, new MissionDtos.ProposeMissionRequest(1L));
            return true;
        } catch (MissionConflictException expected) {
            return false;
        }
    }

    private void deleteMission(long missionId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            outbox.deleteByMissionId(missionId);
            noteRepository.deleteByMissionId(missionId);
            eventRepository.deleteByMissionId(missionId);
            proposalRepository.deleteByMissionId(missionId);
            missionRepository.deleteById(missionId);
        });
    }
}

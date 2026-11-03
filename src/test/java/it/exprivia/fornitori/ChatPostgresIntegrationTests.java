package it.exprivia.fornitori;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.repository.MissionRepository;
import it.exprivia.fornitori.repository.NotificationOutboxRepository;
import it.exprivia.fornitori.service.MissionService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = "fornitori.chat.service-token=token-di-prova")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_PASSWORD", matches = ".+")
class ChatPostgresIntegrationTests {
    private static final String SESSION = "sessione-di-prova-01";
    private static final String PLATE = "pw 660-sw";
    private static final String CHAT = "chat-di-prova";
    private static final String RESPONSABILE = "responsabile@demo.it";
    private static final String AUTISTA = "autista1@demo.it";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MissionService missions;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private NotificationOutboxRepository outbox;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpServletRequestBuilder asBot(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer token-di-prova");
    }

    private ResultActions lookup(String session, String plate) throws Exception {
        return mockMvc.perform(asBot(post("/api/v1/chat/lookup-targa"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sessionId":"%s","targa":"%s"}
                        """.formatted(session, plate)));
    }

    private ResultActions create(String requestId, long customerId, String chat) throws Exception {
        return mockMvc.perform(asBot(post("/api/v1/chat/missioni"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sessionId":"%s","requestId":"%s","clienteId":%d,
                         "tipoIntervento":"riparazione_sul_posto","descrizioneProblema":"Motore in panne",
                         "origineIndirizzo":"Via Roma 1, Molfetta","origineLat":41.2,"origineLng":16.6,
                         "officinaId":null,"canaleOrigine":"telegram","destinatario":"%s"}
                        """.formatted(SESSION, requestId, customerId, chat)));
    }

    private long createdId(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions readStatus(String chat) throws Exception {
        return mockMvc.perform(asBot(post("/api/v1/chat/stato"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"canale":"telegram","destinatario":"%s"}
                        """.formatted(chat)));
    }

    private ResultActions pendingNotifications() throws Exception {
        return mockMvc.perform(asBot(get("/api/v1/chat/notifiche")).param("canale", "telegram"));
    }

    private ResultActions report(long id, String body) throws Exception {
        return mockMvc.perform(asBot(post("/api/v1/chat/notifiche/{id}/esito", id))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long notificationIdFor(String eventKey) {
        return outbox.findByEventKey(eventKey).orElseThrow().getId();
    }

    @Test
    void plateFromTeacherDialogueIsRecognizedWithNameAndCompany() throws Exception {
        lookup(SESSION, PLATE).andExpect(status().isOk())
                .andExpect(jsonPath("$.verificato").value(true))
                .andExpect(jsonPath("$.clienteId").value(1))
                .andExpect(jsonPath("$.clienteNome").value("Lucia Gallo"))
                .andExpect(jsonPath("$.compagniaNome").value("Polizza Sicura Assicurazioni"));
    }

    @Test
    void unknownPlateIsNotVerified() throws Exception {
        lookup(SESSION, "ZZ 000 ZZ").andExpect(status().isOk())
                .andExpect(jsonPath("$.verificato").value(false))
                .andExpect(jsonPath("$.clienteId").isEmpty());
    }

    @Test
    void expiredPolicyIsNotVerified() throws Exception {
        jdbcTemplate.update("UPDATE customers SET scadenza_polizza = DATE '2020-01-01' WHERE id = 1");
        lookup(SESSION, PLATE).andExpect(jsonPath("$.verificato").value(false));
    }

    @Test
    void repeatedUnknownPlatesAreRateLimitedPerSession() throws Exception {
        String session = "sessione-limitata-01";
        for (int attempt = 0; attempt < 5; attempt++) {
            lookup(session, "AA111AA").andExpect(status().isOk());
        }
        lookup(session, PLATE).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("rate_limited"));
        lookup("altra-sessione-di-prova", PLATE).andExpect(jsonPath("$.verificato").value(true));
    }

    @Test
    void chatFollowsMissionUntilItIsClosed() throws Exception {
        long missionId = createdId(create("richiesta-di-prova-01", 1L, CHAT)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stato").value("nuova")));
        assertEquals("telegram", missionRepository.findById(missionId).orElseThrow().getCanaleOrigine());

        readStatus(CHAT).andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("nuova"))
                .andExpect(jsonPath("$.autistaNome").isEmpty());

        missions.propose(RESPONSABILE, missionId, new MissionDtos.ProposeMissionRequest(1L));
        readStatus(CHAT).andExpect(jsonPath("$.stato").value("proposta"))
                .andExpect(jsonPath("$.autistaNome").isEmpty());

        missions.accept(AUTISTA, missionId);
        readStatus(CHAT).andExpect(jsonPath("$.stato").value("accettata"))
                .andExpect(jsonPath("$.autistaNome").value("Luca Bianchi"));

        missions.complete(AUTISTA, missionId, new MissionDtos.CompleteMissionRequest(new BigDecimal("10.00")));
        readStatus(CHAT).andExpect(status().isNotFound());
    }

    @Test
    void statusShowsLatestMissionOfThatChatOnly() throws Exception {
        createdId(create("richiesta-di-prova-02", 1L, CHAT));
        long latest = createdId(create("richiesta-di-prova-03", 1L, CHAT));
        String latestCode = missionRepository.findById(latest).orElseThrow().getCodice();

        readStatus(CHAT).andExpect(jsonPath("$.codice").value(latestCode));
        readStatus("altra-chat").andExpect(status().isNotFound());
    }

    @Test
    void sameRequestIdReturnsSameMission() throws Exception {
        long first = createdId(create("richiesta-di-prova-04", 1L, CHAT).andExpect(status().isCreated()));
        long count = missionRepository.count();
        long second = createdId(create("richiesta-di-prova-04", 1L, CHAT).andExpect(status().isCreated()));
        assertEquals(first, second);
        assertEquals(count, missionRepository.count());
        create("richiesta-di-prova-04", 2L, CHAT).andExpect(status().isConflict());
    }

    @Test
    void unknownCustomerIsRejected() throws Exception {
        create("richiesta-di-prova-05", 999999L, CHAT).andExpect(status().isBadRequest());
    }

    @Test
    void acceptanceAndCompletionQueueNotificationsForTheChat() throws Exception {
        long missionId = createdId(create("richiesta-di-prova-07", 1L, CHAT));
        missions.propose(RESPONSABILE, missionId, new MissionDtos.ProposeMissionRequest(1L));
        pendingNotifications().andExpect(jsonPath("$.notifiche.length()").value(0));

        missions.accept(AUTISTA, missionId);
        pendingNotifications().andExpect(status().isOk())
                .andExpect(jsonPath("$.notifiche.length()").value(1))
                .andExpect(jsonPath("$.notifiche[0].tipo").value("MISSION_ACCEPTED"))
                .andExpect(jsonPath("$.notifiche[0].destinatario").value(CHAT))
                .andExpect(jsonPath("$.notifiche[0].autistaNome").value("Luca Bianchi"));

        long acceptedId = notificationIdFor("MISSION_ACCEPTED:" + missionId);
        report(acceptedId, "{\"inviata\":true}").andExpect(status().isNoContent());
        pendingNotifications().andExpect(jsonPath("$.notifiche.length()").value(0));
        assertEquals("SENT", outbox.findById(acceptedId).orElseThrow().getStato());

        missions.complete(AUTISTA, missionId, new MissionDtos.CompleteMissionRequest(new BigDecimal("10.00")));
        pendingNotifications().andExpect(jsonPath("$.notifiche.length()").value(1))
                .andExpect(jsonPath("$.notifiche[0].tipo").value("MISSION_COMPLETED"));
    }

    @Test
    void failedNotificationStopsAfterFiveAttempts() throws Exception {
        long missionId = createdId(create("richiesta-di-prova-08", 1L, CHAT));
        missions.propose(RESPONSABILE, missionId, new MissionDtos.ProposeMissionRequest(1L));
        missions.accept(AUTISTA, missionId);
        long id = notificationIdFor("MISSION_ACCEPTED:" + missionId);

        for (int attempt = 0; attempt < 4; attempt++) {
            report(id, "{\"inviata\":false,\"errore\":\"HTTP 500\"}").andExpect(status().isNoContent());
        }
        pendingNotifications().andExpect(jsonPath("$.notifiche.length()").value(1));
        report(id, "{\"inviata\":false,\"errore\":\"HTTP 500\"}").andExpect(status().isNoContent());
        pendingNotifications().andExpect(jsonPath("$.notifiche.length()").value(0));
        assertEquals("FAILED", outbox.findById(id).orElseThrow().getStato());

        report(id, "{\"inviata\":true}").andExpect(status().isNoContent());
        assertEquals("FAILED", outbox.findById(id).orElseThrow().getStato());
    }

    @Test
    void missionsCreatedByTheCentreStayInternal() throws Exception {
        MissionDtos.MissionResponse created = missions.create(RESPONSABILE, new MissionDtos.CreateMissionRequest(
                1L, "riparazione_sul_posto", "Chiamata telefonica", "Via Roma 1, Molfetta",
                null, null, null, null, "centrale"));
        missions.propose(RESPONSABILE, created.id(), new MissionDtos.ProposeMissionRequest(1L));
        missions.accept(AUTISTA, created.id());

        pendingNotifications().andExpect(jsonPath("$.notifiche.length()").value(0));
        long id = notificationIdFor("MISSION_ACCEPTED:" + created.id());
        assertEquals("internal", outbox.findById(id).orElseThrow().getChannel());
        report(id, "{\"inviata\":true}").andExpect(status().isNotFound());
    }

    @Test
    void unknownChannelIsRejected() throws Exception {
        mockMvc.perform(asBot(get("/api/v1/chat/notifiche")).param("canale", "internal"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void centreConvertsChatMissionToTowing() throws Exception {
        long missionId = createdId(create("richiesta-di-prova-06", 1L, CHAT));

        assertThrows(ResponseStatusException.class, () -> missions.updateIntervention(
                RESPONSABILE, missionId, new MissionDtos.UpdateInterventionRequest("traino", null)));
        MissionDtos.MissionResponse updated = missions.updateIntervention(
                RESPONSABILE, missionId, new MissionDtos.UpdateInterventionRequest("traino", 1L));
        assertEquals("traino", updated.tipoIntervento());
        assertEquals(1L, updated.officinaId());

        missions.propose(RESPONSABILE, missionId, new MissionDtos.ProposeMissionRequest(1L));
        assertThrows(MissionConflictException.class, () -> missions.updateIntervention(
                RESPONSABILE, missionId, new MissionDtos.UpdateInterventionRequest("riparazione_sul_posto", null)));
    }
}

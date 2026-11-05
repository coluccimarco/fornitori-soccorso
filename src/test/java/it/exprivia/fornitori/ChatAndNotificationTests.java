package it.exprivia.fornitori;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.exprivia.fornitori.service.MapProviders;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;

/** Punti 15 e 16: chatbot, autorizzazioni e notifiche concorrenti. */
@EnabledIfEnvironmentVariable(named = "FORNITORI_TEST_DATABASE", matches = "isolated")
class ChatAndNotificationTests extends IsolatedPostgresTest {
    private static final String CLAIM = "/api/v1/chat/notifiche/claim";
    private static final String TELEGRAM = "{\"canale\":\"telegram\"}";

    @Test
    void chatRequiresTheServiceToken() throws Exception {
        String body = lookupBody(session(), PLATE);
        mvc.perform(post("/api/v1/chat/lookup-targa").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/chat/lookup-targa").header("Authorization", "Bearer sbagliato")
                .contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/chat/lookup-targa").header("Authorization", "Basic " + CHAT_TOKEN)
                .contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        perform(login(RESPONSABILE), post("/api/v1/chat/lookup-targa"), body).andExpect(status().isUnauthorized());
    }

    @Test
    void serviceTokenGivesNoAccessToOperatorApis() throws Exception {
        mvc.perform(get("/api/v1/clienti").header("Authorization", "Bearer " + CHAT_TOKEN)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/missioni").header("Authorization", "Bearer " + CHAT_TOKEN)).andExpect(status().isUnauthorized());
    }

    @Test
    void lookupExposesOnlyNameAndCompany() throws Exception {
        String body = chat(post("/api/v1/chat/lookup-targa"), lookupBody(session(), "pw 660 sw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificato").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat(read(body).propertyNames()).containsExactlyInAnyOrder("verificato", "clienteId", "clienteNome", "compagniaNome", "esito");
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session(), "ZZ000ZZ"))
                .andExpect(jsonPath("$.verificato").value(false))
                .andExpect(jsonPath("$.clienteNome").doesNotExist());
    }

    @Test
    void expiredPolicyIsNotRecognised() throws Exception {
        Object original = jdbc.queryForObject("select scadenza_polizza from customers where id = 1", java.sql.Date.class);
        jdbc.update("update customers set scadenza_polizza = current_date - 1 where id = 1");
        try {
            chat(post("/api/v1/chat/lookup-targa"), lookupBody(session(), PLATE)).andExpect(jsonPath("$.verificato").value(false));
        } finally {
            jdbc.update("update customers set scadenza_polizza = ? where id = 1", original);
        }
    }

    @Test
    void unknownPlatesAreLimitedPerSession() throws Exception {
        String session = session();
        for (int attempt = 0; attempt < 5; attempt++) {
            chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, "XX00" + attempt + "XX")).andExpect(status().isOk());
        }
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, "XX009XX")).andExpect(status().isTooManyRequests());
    }

    @Test
    void aKnownPlateDoesNotResetTheFailureCount() throws Exception {
        String session = session();
        for (int attempt = 0; attempt < 5; attempt++) {
            chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, "XW00" + attempt + "XX")).andExpect(status().isOk());
            if (attempt == 3) {
                chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, PLATE)).andExpect(status().isOk());
            }
        }
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, "XW009XX")).andExpect(status().isTooManyRequests());
    }

    @Test
    void knownPlatesCannotBeHarvestedInOneSession() throws Exception {
        String session = session();
        for (int attempt = 0; attempt < 10; attempt++) {
            chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, PLATE)).andExpect(status().isOk());
        }
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, PLATE)).andExpect(status().isTooManyRequests());
    }

    @Test
    void creationRequiresAFreshLookupForTheSameCustomer() throws Exception {
        String session = session();
        chat(post("/api/v1/chat/missioni"), chatMissionBody(session, session(), 1, "chat-1", "")).andExpect(status().isForbidden());
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, PLATE)).andExpect(status().isOk());
        chat(post("/api/v1/chat/missioni"), chatMissionBody(session, session(), 2, "chat-1", "")).andExpect(status().isForbidden());
        jdbc.update("update chat_lookup_grants set expires_at = now() - interval '1 second'");
        chat(post("/api/v1/chat/missioni"), chatMissionBody(session, session(), 1, "chat-1", "")).andExpect(status().isForbidden());
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, PLATE)).andExpect(status().isOk());
        chat(post("/api/v1/chat/missioni"), chatMissionBody(session, session(), 1, "chat-1", ""))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.stato").value("nuova"));
    }

    @Test
    void aLookupAuthorizesOnlyOneNewRequestButRetriesStayIdempotent() throws Exception {
        String session = session();
        String request = session();
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, PLATE)).andExpect(status().isOk());
        String first = chat(post("/api/v1/chat/missioni"), chatMissionBody(session, request, 1, "chat-1", ""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String retry = chat(post("/api/v1/chat/missioni"), chatMissionBody(session, request, 1, "chat-1", ""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(retry).isEqualTo(first);
        chat(post("/api/v1/chat/missioni"), chatMissionBody(session, request, 1, "chat-2", "")).andExpect(status().isConflict());
        chat(post("/api/v1/chat/missioni"), chatMissionBody(session, session(), 1, "chat-1", "")).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from chat_mission_requests", Integer.class)).isEqualTo(1);
    }

    @Test
    void geocoderIsNotCalledWithoutAValidLookup() throws Exception {
        String body = chatMissionBody(session(), session(), 1, "chat-1", "").replace("riparazione_sul_posto", "traino");
        chat(post("/api/v1/chat/missioni"), body).andExpect(status().isForbidden());
        verify(geocoding, never()).geocode(anyString());
    }

    @Test
    void towingFromChatGetsTheNearestWorkshop() throws Exception {
        when(geocoding.geocode(anyString())).thenReturn(Optional.of(
                new MapProviders.GeoPoint(new BigDecimal("41.2"), new BigDecimal("16.6"))));
        String session = session();
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, PLATE)).andExpect(status().isOk());
        String body = chatMissionBody(session, session(), 1, "chat-1", "").replace("riparazione_sul_posto", "traino");
        chat(post("/api/v1/chat/missioni"), body).andExpect(status().isCreated());
        Long expected = jdbc.queryForObject("select id from workshops order by "
                + "(lat - 41.2) ^ 2 + ((lng - 16.6) * cos(radians(41.2))) ^ 2 limit 1", Long.class);
        assertThat(jdbc.queryForObject("select workshop_id from missions where not is_fixture", Long.class)).isEqualTo(expected);
    }

    @Test
    void statusIsLimitedToTheChatAndNeverRevealsTheDriver() throws Exception {
        long mission = chatMission("chat-1");
        chat(post("/api/v1/chat/stato"), "{\"canale\":\"telegram\",\"destinatario\":\"chat-altra\"}").andExpect(status().isNotFound());
        String status = chat(post("/api/v1/chat/stato"), "{\"canale\":\"telegram\",\"destinatario\":\"chat-1\"}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(read(status).get("autistaNome").isNull()).isTrue();
        Actor manager = login(RESPONSABILE);
        propose(manager, mission, 1);
        perform(login(AUTISTA_1), post("/api/v1/missioni/{id}/accept", mission)).andExpect(status().isOk());
        chat(post("/api/v1/chat/stato"), "{\"canale\":\"telegram\",\"destinatario\":\"chat-1\"}")
                .andExpect(jsonPath("$.stato").value("accettata"))
                .andExpect(jsonPath("$.autistaNome").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void acceptanceQueuesOneNotificationAndClaimReservesIt() throws Exception {
        long id = acceptedChatMissionNotification();
        JsonNode claimed = claim();
        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).get("tipo").asString()).isEqualTo("MISSION_ACCEPTED");
        assertThat(claimed.get(0).get("autistaNome").isNull()).isTrue();
        assertThat(state(id)).isEqualTo("SENDING");
        assertThat(claim()).isEmpty();
        report(id, claimed.get(0).get("tentativo").asInt(), true);
        assertThat(state(id)).isEqualTo("SENT");
        assertThat(claim()).isEmpty();
    }

    @Test
    void concurrentDispatchersNeverReceiveTheSameNotification() throws Exception {
        Set<Long> ids = new HashSet<>();
        for (String recipient : List.of("chat-1", "chat-2", "chat-3", "chat-4")) {
            ids.add(acceptedChatMissionNotification(recipient));
        }
        List<Long> delivered = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Future<JsonNode>> calls = new ArrayList<>();
            for (int caller = 0; caller < 4; caller++) {
                calls.add(executor.submit(this::claim));
            }
            for (Future<JsonNode> call : calls) {
                call.get().forEach(item -> delivered.add(item.get("id").asLong()));
            }
        }
        assertThat(delivered).doesNotHaveDuplicates();
        assertThat(new HashSet<>(delivered)).isEqualTo(ids);
    }

    @Test
    void aStaleDispatcherCannotOverwriteTheCurrentAttempt() throws Exception {
        long id = acceptedChatMissionNotification();
        JsonNode first = claim();
        int staleAttempt = first.get(0).get("tentativo").asInt();
        jdbc.update("update notifications_outbox set prossimo_tentativo_il = now() - interval '1 second' where id = ?", id);

        JsonNode second = claim();
        assertThat(second).hasSize(1);
        int currentAttempt = second.get(0).get("tentativo").asInt();
        assertThat(currentAttempt).isGreaterThan(staleAttempt);

        report(id, staleAttempt, false);
        assertThat(state(id)).isEqualTo("SENDING");
        assertThat(attempts(id)).isEqualTo(currentAttempt);
        report(id, staleAttempt, true);
        assertThat(state(id)).isEqualTo("SENDING");

        report(id, currentAttempt, true);
        assertThat(state(id)).isEqualTo("SENT");
        report(id, currentAttempt, false);
        assertThat(state(id)).isEqualTo("SENT");
    }

    @Test
    void failuresRetryAfterADelayAndStopAfterFiveAttempts() throws Exception {
        long id = acceptedChatMissionNotification();
        for (int attempt = 0; attempt < 5; attempt++) {
            JsonNode claimed = claim();
            assertThat(claimed).hasSize(1);
            report(id, claimed.get(0).get("tentativo").asInt(), false);
            if (attempt < 4) {
                assertThat(state(id)).isEqualTo("PENDING");
                assertThat(claim()).isEmpty();
                jdbc.update("update notifications_outbox set prossimo_tentativo_il = now() - interval '1 second' where id = ?", id);
            }
        }
        assertThat(state(id)).isEqualTo("FAILED");
        assertThat(claim()).isEmpty();
    }

    @Test
    void centreMissionsKeepNoPhoneInTheOutboxAndAreNeverDelivered() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        propose(manager, mission, 1);
        perform(login(AUTISTA_1), post("/api/v1/missioni/{id}/accept", mission)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select stato || ':' || destinatario from notifications_outbox where mission_id = ?",
                String.class, mission)).isEqualTo("SKIPPED:internal");
        assertThat(claim()).isEmpty();
        chat(post(CLAIM), "{\"canale\":\"internal\"}").andExpect(status().isBadRequest());
        Long row = jdbc.queryForObject("select id from notifications_outbox where mission_id = ?", Long.class, mission);
        chat(post("/api/v1/chat/notifiche/{id}/esito", row), "{\"inviata\":true,\"tentativo\":0}").andExpect(status().isNotFound());
    }

    private long chatMission(String recipient) throws Exception {
        String session = session();
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, PLATE)).andExpect(status().isOk());
        return read(chat(post("/api/v1/chat/missioni"), chatMissionBody(session, session(), 1, recipient, ""))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private long acceptedChatMissionNotification() throws Exception {
        return acceptedChatMissionNotification("chat-1");
    }

    private int nextDriver = 1;

    private long acceptedChatMissionNotification(String recipient) throws Exception {
        long mission = chatMission(recipient);
        int driver = nextDriver++;
        propose(login(RESPONSABILE), mission, driver);
        perform(login("autista" + driver + "@demo.it"), post("/api/v1/missioni/{id}/accept", mission)).andExpect(status().isOk());
        return jdbc.queryForObject("select id from notifications_outbox where mission_id = ?", Long.class, mission);
    }

    private JsonNode claim() throws Exception {
        return read(chat(post(CLAIM), TELEGRAM).andExpect(status().isOk()).andReturn()).get("notifiche");
    }

    private void report(long id, int attempt, boolean sent) throws Exception {
        chat(post("/api/v1/chat/notifiche/{id}/esito", id),
                "{\"inviata\":" + sent + ",\"tentativo\":" + attempt + (sent ? "" : ",\"errore\":\"HTTP 500\"") + "}")
                .andExpect(status().isNoContent());
    }

    private String state(long id) {
        return jdbc.queryForObject("select stato from notifications_outbox where id = ?", String.class, id);
    }

    private int attempts(long id) {
        return jdbc.queryForObject("select tentativi from notifications_outbox where id = ?", Integer.class, id);
    }

    private JsonNode read(String body) {
        return json.readTree(body);
    }
}

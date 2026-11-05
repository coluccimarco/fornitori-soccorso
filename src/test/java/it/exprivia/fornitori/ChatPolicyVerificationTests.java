package it.exprivia.fornitori;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

/** Formato nuovo delle richieste da chat: tipo di problema, telefono sul posto e polizza da verificare. */
@EnabledIfEnvironmentVariable(named = "FORNITORI_TEST_DATABASE", matches = "isolated")
class ChatPolicyVerificationTests extends IsolatedPostgresTest {
    private static final String UNKNOWN_PLATE = "ZZ999ZZ";
    private static final String STATUS = "{\"canale\":\"telegram\",\"destinatario\":\"chat-1\"}";

    @Test
    void theLookupTellsVerifiedFromToBeVerified() throws Exception {
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session(), PLATE))
                .andExpect(jsonPath("$.esito").value("VERIFICATA")).andExpect(jsonPath("$.verificato").value(true));
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session(), UNKNOWN_PLATE))
                .andExpect(jsonPath("$.esito").value("DA_VERIFICARE"))
                .andExpect(jsonPath("$.verificato").value(false))
                .andExpect(jsonPath("$.clienteId").value(nullValue()));
    }

    @ParameterizedTest
    @CsvSource({
        "RUOTA_CON_SCORTA,riparazione_sul_posto,Ruota forata con ruota di scorta",
        "RUOTA_SENZA_SCORTA,traino,Ruota forata senza ruota di scorta",
        "BATTERIA_SCARICA,riparazione_sul_posto,Batteria scarica",
        "GUASTO_MOTORE,traino,Guasto motore",
        "INCIDENTE,traino,Incidente"})
    void theBackendDecidesTheInterventionFromTheProblem(String problem, String intervention, String label) throws Exception {
        String session = lookedUp(PLATE);
        // Tipo e descrizione inviati dal bot sono ignorati.
        String body = verifiedBody(session, session(), problem).replace("}",
                ",\"tipoIntervento\":\"altro\",\"descrizioneProblema\":\"altro\"}");
        long id = read(chat(post("/api/v1/chat/missioni"), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inVerifica").value(false))
                .andExpect(jsonPath("$.stato").value("nuova")).andReturn()).get("id").asLong();
        JsonNode mission = read(perform(login(RESPONSABILE), get("/api/v1/missioni/{id}", id)).andReturn());
        assertThat(mission.get("tipoIntervento").asString()).isEqualTo(intervention);
        assertThat(mission.get("descrizioneProblema").asString()).isEqualTo(label);
        assertThat(mission.get("tipoProblema").asString()).isEqualTo(problem);
        assertThat(mission.get("telefonoSulPosto").asString()).isEqualTo("333 1234567");
        assertThat(mission.get("targaDichiarata").asString()).isEqualTo(PLATE);
        assertThat(mission.get("officinaId").isNull()).isEqualTo("riparazione_sul_posto".equals(intervention));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"telefonoSulPosto\":\"abc\"", "\"telefonoSulPosto\":\"12\"", "\"tipoProblema\":\"BOH\""})
    void invalidFieldsAreRejected(String override) throws Exception {
        String field = override.substring(0, override.indexOf(':'));
        String body = verifiedBody(lookedUp(PLATE), session(), "INCIDENTE").replaceFirst(field + ":\"[^\"]*\"", override);
        chat(post("/api/v1/chat/missioni"), body).andExpect(status().isBadRequest());
    }

    @Test
    void newRequestsNeedPlateAndPhone() throws Exception {
        String session = lookedUp(PLATE);
        chat(post("/api/v1/chat/missioni"), verifiedBody(session, session(), "INCIDENTE").replace("\"targa\":\"" + PLATE + "\",", ""))
                .andExpect(status().isBadRequest());
        chat(post("/api/v1/chat/missioni"), verifiedBody(session, session(), "INCIDENTE").replace("\"telefonoSulPosto\":\"333 1234567\",", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theGrantIsBoundToThePlateAndToTheCustomer() throws Exception {
        String session = lookedUp(PLATE);
        chat(post("/api/v1/chat/missioni"), verifiedBody(session, session(), "BATTERIA_SCARICA").replace(PLATE, "AB123CD"))
                .andExpect(status().isForbidden());
        chat(post("/api/v1/chat/missioni"), unverifiedBody(session, session()).replace(UNKNOWN_PLATE, PLATE))
                .andExpect(status().isForbidden());
        String other = lookedUp(UNKNOWN_PLATE);
        chat(post("/api/v1/chat/missioni"), unverifiedBody(other, session()).replace(UNKNOWN_PLATE, "AB123CD"))
                .andExpect(status().isForbidden());
        chat(post("/api/v1/chat/missioni"), verifiedBody(other, session(), "BATTERIA_SCARICA").replace(PLATE, UNKNOWN_PLATE))
                .andExpect(status().isForbidden());
    }

    @Test
    void textThatIsNotAPlateOpensNoRequest() throws Exception {
        String session = session();
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, "-- --")).andExpect(jsonPath("$.esito").value("DA_VERIFICARE"));
        assertThat(jdbc.queryForObject("select count(*) from chat_lookup_grants where session_id = ?", Integer.class, session)).isZero();
    }

    @Test
    void anUnverifiablePolicyWaitsForTheCentreWhichConfirmsIt() throws Exception {
        String session = lookedUp(UNKNOWN_PLATE);
        String request = session();
        String body = unverifiedBody(session, request);
        long id = createUnverified(body);
        // Un lookup vale per una sola pratica, ma un nuovo invio della stessa richiesta resta idempotente.
        chat(post("/api/v1/chat/missioni"), body).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
        chat(post("/api/v1/chat/missioni"), unverifiedBody(session, session())).andExpect(status().isForbidden());
        chat(post("/api/v1/chat/stato"), STATUS).andExpect(jsonPath("$.stato").value("verifica_polizza"));

        Actor manager = login(RESPONSABILE);
        perform(manager, get("/api/v1/dashboard"))
                .andExpect(jsonPath("$.categorie.verificaPolizza").value(1))
                .andExpect(jsonPath("$.categorie.critiche").value(1))
                .andExpect(jsonPath("$.codaCritica[0].motivo").value("VERIFICA_POLIZZA"))
                .andExpect(jsonPath("$.codaCritica[0].targa").value(UNKNOWN_PLATE))
                .andExpect(jsonPath("$.codaCritica[0].telefonoSulPosto").value("333 1234567"))
                .andExpect(jsonPath("$.codaCritica[0].clienteNome").value(nullValue()));
        perform(manager, get("/api/v1/missioni").param("stato", "verifica_polizza"))
                .andExpect(jsonPath("$[0].clienteId").value(nullValue()));
        perform(manager, post("/api/v1/missioni/{id}/proposte", id), "{\"autistaId\":1}").andExpect(status().isConflict());

        String verify = "/api/v1/missioni/{id}/verifica-polizza";
        perform(login(AUTISTA_1), post(verify, id), "{\"esito\":\"RESPINTA\"}").andExpect(status().isForbidden());
        perform(manager, post(verify, id), "{\"esito\":\"BOH\"}").andExpect(status().isBadRequest());
        perform(manager, post(verify, id), "{\"esito\":\"CONFERMATA\"}").andExpect(status().isBadRequest());
        perform(manager, post(verify, id), "{\"esito\":\"CONFERMATA\",\"clienteId\":99999}").andExpect(status().isBadRequest());
        Object policy = jdbc.queryForObject("select scadenza_polizza from customers where id = 1", java.sql.Date.class);
        jdbc.update("update customers set scadenza_polizza = current_date - 1 where id = 1");
        try {
            perform(manager, post(verify, id), "{\"esito\":\"CONFERMATA\",\"clienteId\":1}").andExpect(status().isConflict());
        } finally {
            jdbc.update("update customers set scadenza_polizza = ? where id = 1", policy);
        }
        perform(manager, post(verify, id), "{\"esito\":\"CONFERMATA\",\"clienteId\":1,\"note\":\"Sentita la compagnia\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("nuova"))
                .andExpect(jsonPath("$.clienteId").value(1));
        perform(manager, post(verify, id), "{\"esito\":\"RESPINTA\"}").andExpect(status().isConflict());
        // Il bot può ripetere la richiesta originale, senza cliente, anche dopo la conferma.
        chat(post("/api/v1/chat/missioni"), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.inVerifica").value(false));
    }

    @Test
    void aRejectedPolicyCancelsAtNoCostAndNotifiesTheChat() throws Exception {
        long id = createUnverified(unverifiedBody(lookedUp(UNKNOWN_PLATE), session()));
        perform(login(RESPONSABILE), post("/api/v1/missioni/{id}/verifica-polizza", id), "{\"esito\":\"RESPINTA\",\"note\":\"Nessuna copertura\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("annullata"))
                .andExpect(jsonPath("$.annullataDa").value("centrale"))
                .andExpect(jsonPath("$.motivoAnnullamento").value("Nessuna copertura"))
                .andExpect(jsonPath("$.costoFinale").value(0))
                .andExpect(jsonPath("$.autistaId").value(nullValue()));
        chat(post("/api/v1/chat/notifiche/claim"), "{\"canale\":\"telegram\"}")
                .andExpect(jsonPath("$.notifiche[0].tipo").value("MISSION_CANCELLED"))
                .andExpect(jsonPath("$.notifiche[0].destinatario").value("chat-1"))
                .andExpect(jsonPath("$.notifiche[0].autistaNome").value(nullValue()));
        chat(post("/api/v1/chat/stato"), STATUS).andExpect(jsonPath("$.stato").value("annullata"));
    }

    @Test
    void anAcceptedNotificationAndTheStatusNeverNameTheDriver() throws Exception {
        long id = read(chat(post("/api/v1/chat/missioni"), verifiedBody(lookedUp(PLATE), session(), "BATTERIA_SCARICA"))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        propose(login(RESPONSABILE), id, 1);
        perform(login(AUTISTA_1), post("/api/v1/missioni/{id}/accept", id)).andExpect(status().isOk());
        String driver = jdbc.queryForObject("select nome from drivers where id = 1", String.class);
        assertThat(jdbc.queryForObject("select payload::text from notifications_outbox where mission_id = ?", String.class, id))
                .doesNotContain(driver);
        chat(post("/api/v1/chat/notifiche/claim"), "{\"canale\":\"telegram\"}")
                .andExpect(jsonPath("$.notifiche[0].tipo").value("MISSION_ACCEPTED"))
                .andExpect(jsonPath("$.notifiche[0].autistaNome").value(nullValue()));
        chat(post("/api/v1/chat/stato"), STATUS)
                .andExpect(jsonPath("$.stato").value("accettata"))
                .andExpect(jsonPath("$.autistaNome").value(nullValue()));
    }

    @Test
    void cancellingAChatMissionQueuesOneNotificationAndACentreMissionNone() throws Exception {
        Actor manager = login(RESPONSABILE);
        long chatMission = read(chat(post("/api/v1/chat/missioni"), verifiedBody(lookedUp(PLATE), session(), "BATTERIA_SCARICA"))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        perform(manager, post("/api/v1/missioni/{id}/annulla", chatMission), "{\"motivo\":\"Risolto\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select stato from notifications_outbox where mission_id = ? and event_key like 'MISSION_CANCELLED%'",
                String.class, chatMission)).isEqualTo("PENDING");

        long centreMission = createMission(manager);
        perform(manager, post("/api/v1/missioni/{id}/annulla", centreMission), "{\"motivo\":\"Risolto\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select stato from notifications_outbox where mission_id = ?", String.class, centreMission))
                .isEqualTo("SKIPPED");
    }

    @Test
    void theOldBotFormatStillWorksDuringTheMerge() throws Exception {
        String session = lookedUp(PLATE);
        chat(post("/api/v1/chat/missioni"), chatMissionBody(session, session(), 1, "chat-1", ""))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.stato").value("nuova"));
        String incomplete = "{\"sessionId\":\"" + lookedUp(PLATE) + "\",\"requestId\":\"" + session() + "\",\"clienteId\":1,"
                + "\"origineIndirizzo\":\"Via Roma 1\",\"canaleOrigine\":\"telegram\",\"destinatario\":\"chat-1\"}";
        chat(post("/api/v1/chat/missioni"), incomplete).andExpect(status().isBadRequest());
    }

    private long createUnverified(String body) throws Exception {
        return read(chat(post("/api/v1/chat/missioni"), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stato").value("verifica_polizza"))
                .andExpect(jsonPath("$.inVerifica").value(true))
                .andReturn()).get("id").asLong();
    }

    private String lookedUp(String plate) throws Exception {
        String session = session();
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, plate)).andExpect(status().isOk());
        return session;
    }

    private String verifiedBody(String session, String request, String problem) {
        return body(session, request, "\"clienteId\":1,\"targa\":\"" + PLATE + "\",", problem);
    }

    private String unverifiedBody(String session, String request) {
        return body(session, request, "\"targa\":\"" + UNKNOWN_PLATE + "\",", "BATTERIA_SCARICA");
    }

    private static String body(String session, String request, String identity, String problem) {
        return "{\"sessionId\":\"" + session + "\",\"requestId\":\"" + request + "\"," + identity
                + "\"telefonoSulPosto\":\"333 1234567\",\"tipoProblema\":\"" + problem + "\","
                + "\"origineIndirizzo\":\"Via Roma 1, Molfetta\",\"origineLat\":41.2,\"origineLng\":16.6,"
                + "\"canaleOrigine\":\"telegram\",\"destinatario\":\"chat-1\"}";
    }
}

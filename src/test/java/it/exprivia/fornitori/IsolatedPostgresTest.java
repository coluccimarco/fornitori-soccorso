package it.exprivia.fornitori;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.exprivia.fornitori.service.MapProviders;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Base per i test su PostgreSQL. Cancella i dati live prima di ogni test, quindi ogni classe figlia deve
 * dichiarare @EnabledIfEnvironmentVariable(FORNITORI_TEST_DATABASE=isolated): JUnit non la eredita da qui.
 * In più si ferma se il database non si chiama fornitori_e2e*, prima di qualsiasi scrittura.
 */
@SpringBootTest(properties = {
        "fornitori.chat.service-token=" + IsolatedPostgresTest.CHAT_TOKEN,
        "fornitori.missions.expiry-delay-ms=3600000"})
@AutoConfigureMockMvc
abstract class IsolatedPostgresTest {
    static final String CHAT_TOKEN = "token-di-prova-per-i-test-0123456789abcdef";
    static final String PASSWORD = "Prova-" + UUID.randomUUID();
    static final String RESPONSABILE = "responsabile@demo.it";
    static final String AUTISTA_1 = "autista1@demo.it";
    static final String AUTISTA_2 = "autista2@demo.it";
    static final String PLATE = "PW660SW";
    private static final AtomicInteger ADDRESSES = new AtomicInteger();
    private static boolean passwordsReady;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @MockitoBean MapProviders.GeocodingClient geocoding;
    @MockitoBean MapProviders.RoutingClient routing;

    /** Ogni test ha un proprio indirizzo: i limiti per IP di un test non influenzano gli altri. */
    String remoteAddress;

    @BeforeEach
    void isolate() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).startsWith("fornitori_e2e");
        for (String table : new String[] {"notifications_outbox", "mission_notes", "mission_events",
                "mission_proposals", "chat_mission_requests"}) {
            jdbc.update("delete from " + table + " where mission_id in (select id from missions where not is_fixture)");
        }
        jdbc.update("delete from missions where not is_fixture");
        jdbc.update("delete from chat_lookup_grants");
        jdbc.update("delete from customers where id > 40");
        if (!passwordsReady) {
            jdbc.update("update app_users set password_hash = ?", encoder.encode(PASSWORD));
            passwordsReady = true;
        }
        int next = ADDRESSES.incrementAndGet();
        remoteAddress = "10.20." + (next / 250) + "." + (next % 250 + 1);
    }

    record Actor(MockHttpSession session, String csrf) {
    }

    Actor anonymous() throws Exception {
        MockHttpSession session = new MockHttpSession();
        return new Actor(session, csrf(session));
    }

    Actor login(String email) throws Exception {
        Actor start = anonymous();
        perform(start, post("/api/v1/auth/login"), "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isOk());
        return new Actor(start.session(), csrf(start.session()));
    }

    String csrf(MockHttpSession session) throws Exception {
        return read(mvc.perform(get("/api/v1/auth/csrf").session(session)).andReturn()).get("token").asString();
    }

    ResultActions perform(Actor actor, MockHttpServletRequestBuilder request, String body) throws Exception {
        MockHttpServletRequestBuilder prepared = request.session(actor.session())
                .header("X-CSRF-TOKEN", actor.csrf())
                .with(address -> {
                    address.setRemoteAddr(remoteAddress);
                    return address;
                });
        if (body != null) {
            prepared = prepared.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(prepared);
    }

    ResultActions perform(Actor actor, MockHttpServletRequestBuilder request) throws Exception {
        return perform(actor, request, null);
    }

    ResultActions chat(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + CHAT_TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    long createMission(Actor manager) throws Exception {
        String body = """
                {"clienteId":1,"tipoIntervento":"riparazione_sul_posto","descrizioneProblema":"Non parte",
                 "origineIndirizzo":"Via Roma 1, Molfetta","origineLat":41.2,"origineLng":16.6,"canaleOrigine":"centrale"}""";
        return read(perform(manager, post("/api/v1/missioni"), body).andExpect(status().isCreated()).andReturn())
                .get("id").asLong();
    }

    void propose(Actor manager, long missionId, long driverId) throws Exception {
        perform(manager, post("/api/v1/missioni/{id}/proposte", missionId), "{\"autistaId\":" + driverId + "}")
                .andExpect(status().isOk());
    }

    /** Sposta la scadenza nel passato: su un database isolato evita di aspettare 10 minuti. */
    void expireProposal(long missionId) {
        jdbc.update("update mission_proposals set proposta_il = now() - interval '11 minutes', "
                + "scadenza_il = now() - interval '1 minute' where mission_id = ? and stato = 'proposta'", missionId);
        jdbc.update("update missions set proposta_il = now() - interval '11 minutes', "
                + "scadenza_il = now() - interval '1 minute' where id = ?", missionId);
    }

    long fixtureMission() {
        return jdbc.queryForObject("select min(m.id) from missions m join mission_proposals p on p.mission_id = m.id "
                + "where m.is_fixture", Long.class);
    }

    String session() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    String lookupBody(String session, String plate) {
        return "{\"sessionId\":\"" + session + "\",\"targa\":\"" + plate + "\"}";
    }

    String chatMissionBody(String session, String request, long customer, String recipient, String extra) {
        return "{\"sessionId\":\"" + session + "\",\"requestId\":\"" + request + "\",\"clienteId\":" + customer
                + ",\"tipoIntervento\":\"riparazione_sul_posto\",\"descrizioneProblema\":\"Non parte\","
                + "\"origineIndirizzo\":\"Via Roma 1, Molfetta\",\"canaleOrigine\":\"telegram\","
                + "\"destinatario\":\"" + recipient + "\"" + extra + "}";
    }
}

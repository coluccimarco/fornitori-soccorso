package it.exprivia.fornitori;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "fornitori.missions.expiry-scheduler-enabled=false")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_PASSWORD", matches = ".+")
class PostgresHttpWorkflowTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PasswordEncoder passwords;
    @MockitoBean Clock clock;
    private String password;

    @BeforeEach void setup() {
        password = UUID.randomUUID().toString();
        jdbc.update("update app_users set password_hash = ?", passwords.encode(password));
        when(clock.instant()).thenReturn(Instant.parse("2026-11-04T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    private String token(MockHttpSession session) throws Exception {
        return JsonPath.read(mvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    private MockHttpSession login(String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();
        String csrf = token(session);
        mvc.perform(post("/api/v1/auth/login").session(session).header("X-CSRF-TOKEN", csrf)
                .contentType("application/json").content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertNotEquals(before, session.getId());
        mvc.perform(post("/api/v1/auth/logout").session(session).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isForbidden());
        return session;
    }

    private ResultActions send(MockHttpSession session, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.session(session).header("X-CSRF-TOKEN", token(session))
                .contentType("application/json").content(body));
    }

    private long create(MockHttpSession manager) throws Exception {
        String response = send(manager, post("/api/v1/missioni"), """
            {"clienteId":1,"tipoIntervento":"riparazione_sul_posto","descrizioneProblema":"Workflow",
             "origineIndirizzo":"Via Roma 1","canaleOrigine":"centrale"}
            """).andExpect(status().isCreated()).andExpect(jsonPath("$.stato").value("nuova"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private void propose(MockHttpSession manager, long id, int driver) throws Exception {
        send(manager, post("/api/v1/missioni/{id}/proposte", id), "{\"autistaId\":" + driver + "}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.offertaId").isNumber())
                .andExpect(jsonPath("$.scadenzaIl").isString());
    }

    private String persistedStatus(long id) {
        em.flush(); em.clear();
        return jdbc.queryForObject("select stato from missions where id = ?", String.class, id);
    }

    @Test void realAuthenticationRegistryAndMissionRoundTrip() throws Exception {
        MockHttpSession manager = login("responsabile@demo.it");
        for (String path : new String[]{"autisti", "mezzi", "compagnie", "clienti", "officine"}) {
            mvc.perform(get("/api/v1/" + path).session(manager)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").isNumber());
        }
        String customer = send(manager, post("/api/v1/clienti"), """
            {"nome":"Cliente workflow","telefono":"123","targa":"TESTHTTP1","compagniaId":1,
             "numeroPolizza":"TEST","scadenzaPolizza":"2030-12-31"}
            """).andExpect(status().isCreated()).andExpect(jsonPath("$.compagniaNome").isString())
                .andReturn().getResponse().getContentAsString();
        em.flush(); em.clear();
        assertEquals("Cliente workflow", jdbc.queryForObject("select nome from customers where id = ?", String.class,
                ((Number) JsonPath.read(customer, "$.id")).longValue()));
        mvc.perform(get("/api/v1/clienti?search=TESTHTTP1").session(manager))
                .andExpect(jsonPath("$[0].nome").value("Cliente workflow"));
        mvc.perform(get("/api/v1/clienti?search=!!!").session(manager)).andExpect(jsonPath("$.length()").value(0));
        long id = create(manager);
        assertEquals("nuova", persistedStatus(id));
        propose(manager, id, 1);
        MockHttpSession driver = login("autista1@demo.it");
        mvc.perform(get("/api/v1/autista/missioni").session(driver))
                .andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].clienteTelefono").doesNotExist())
                .andExpect(jsonPath("$[0].clienteNome").doesNotExist());
        send(driver, post("/api/v1/missioni/{id}/accept", id), "").andExpect(status().isOk());
        assertEquals("accettata", persistedStatus(id));
        mvc.perform(get("/api/v1/autista/missioni").session(driver))
                .andExpect(jsonPath("$[0].clienteTelefono").isString()).andExpect(jsonPath("$[0].clienteNome").isString());
        send(driver, post("/api/v1/missioni/{id}/notes", id), "{\"testo\":\"In viaggio\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.testo").value("In viaggio"));
        send(driver, post("/api/v1/missioni/{id}/complete", id), "{\"kmEffettivi\":18.25}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.costoFinale").value(71.90));
        assertEquals("eseguita", persistedStatus(id));
        assertEquals(new BigDecimal("71.90"), jdbc.queryForObject("select costo_finale from missions where id = ?", BigDecimal.class, id));
        assertEquals(2, jdbc.queryForObject("select count(*) from notifications_outbox where mission_id = ?", Integer.class, id));
        send(driver, post("/api/v1/missioni/{id}/complete", id), "{\"kmEffettivi\":18.25}")
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/autista/storico").session(driver)).andExpect(jsonPath("$.totaleEur").value(71.90));
        send(manager, patch("/api/v1/missioni/{id}/cost", id), "{\"costoFinale\":70.50,\"motivo\":\"Rettifica test\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.costoCalcolato").value(71.90));
        mvc.perform(get("/api/v1/dashboard").session(manager)).andExpect(jsonPath("$.totaleEur").value(70.50));
        send(manager, post("/api/v1/auth/logout"), "").andExpect(status().isNoContent());
        assertTrue(manager.isInvalid());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void formerDriverCannotReadNotesOrRouteAfterReassignment() throws Exception {
        MockHttpSession manager = login("responsabile@demo.it");
        MockHttpSession first = login("autista1@demo.it");
        long id = create(manager); propose(manager, id, 1);
        send(first, post("/api/v1/missioni/{id}/reject", id), "").andExpect(status().isOk());
        propose(manager, id, 2);
        for (String suffix : new String[]{"", "/notes"}) {
            mvc.perform(get("/api/v1/missioni/" + id + suffix).session(first)).andExpect(status().isForbidden());
        }
        send(first, post("/api/v1/missioni/{id}/notes", id), "{\"testo\":\"denied\"}").andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/maps/route").session(first).header("X-CSRF-TOKEN", token(first)).param("missionId", Long.toString(id)))
                .andExpect(status().isForbidden());
        assertEquals("proposta", persistedStatus(id));
        assertEquals(2, jdbc.queryForObject("select count(*) from mission_proposals where mission_id = ?", Integer.class, id));
    }

    @Test void rejectionAtDeadlineExpiresWithoutWaitingForScheduler() throws Exception {
        MockHttpSession manager = login("responsabile@demo.it");
        MockHttpSession driver = login("autista1@demo.it");
        long id = create(manager); propose(manager, id, 1);
        when(clock.instant()).thenReturn(Instant.parse("2026-11-04T12:10:00Z"));
        send(driver, post("/api/v1/missioni/{id}/reject", id), "").andExpect(status().isConflict());
        assertEquals("scaduta", persistedStatus(id));
        propose(manager, id, 2);
        assertEquals(2, jdbc.queryForObject("select count(*) from mission_proposals where mission_id = ?", Integer.class, id));
    }

    @Test void calculatedCostOverflowDoesNotCloseMission() throws Exception {
        MockHttpSession manager = login("responsabile@demo.it");
        MockHttpSession driver = login("autista1@demo.it");
        long id = create(manager); propose(manager, id, 1);
        send(driver, post("/api/v1/missioni/{id}/accept", id), "").andExpect(status().isOk());
        send(driver, post("/api/v1/missioni/{id}/complete", id), "{\"kmEffettivi\":99999999.99}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_request"));
        assertEquals("accettata", persistedStatus(id));
        mvc.perform(get("/api/v1/missioni/999999999").session(manager)).andExpect(status().isNotFound());
    }

    @Test void databaseRejectsHalfCoordinatePair() throws Exception {
        MockHttpSession manager = login("responsabile@demo.it");
        long id = create(manager);
        em.flush();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("update missions set origine_lat=41.2, origine_lng=null where id=?", id));
    }
}

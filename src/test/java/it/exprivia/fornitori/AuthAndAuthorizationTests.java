package it.exprivia.fornitori;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Punti 1-5 e 14: login, ruoli, sessione, CSRF, accesso agli oggetti e dati personali. */
@EnabledIfEnvironmentVariable(named = "FORNITORI_TEST_DATABASE", matches = "isolated")
class AuthAndAuthorizationTests extends IsolatedPostgresTest {

    @Test
    void validLoginReturnsOnlyTheProfile() throws Exception {
        Actor start = anonymous();
        String body = perform(start, post("/api/v1/auth/login"),
                "{\"email\":\"" + RESPONSABILE + "\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruolo").value("responsabile"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body.toLowerCase()).doesNotContain("password", "hash");
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameAnswer() throws Exception {
        Actor start = anonymous();
        String wrong = perform(start, post("/api/v1/auth/login"),
                "{\"email\":\"" + RESPONSABILE + "\",\"password\":\"sbagliata\"}")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknown = perform(start, post("/api/v1/auth/login"),
                "{\"email\":\"nessuno@demo.it\",\"password\":\"sbagliata\"}")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(wrong).isEqualTo(unknown).isEqualTo("{\"status\":401,\"error\":\"unauthorized\"}");
    }

    @Test
    void loginWithoutCsrfIsRefused() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"" + RESPONSABILE + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tooManyLoginsFromOneAddressAreLimited() throws Exception {
        Actor start = anonymous();
        for (int attempt = 0; attempt < 20; attempt++) {
            perform(start, post("/api/v1/auth/login"),
                    "{\"email\":\"utente" + attempt + "@demo.it\",\"password\":\"x\"}")
                    .andExpect(status().isUnauthorized());
        }
        perform(start, post("/api/v1/auth/login"), "{\"email\":\"altro@demo.it\",\"password\":\"x\"}")
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void repeatedFailuresOnOneAccountAreLimitedAcrossAddresses() throws Exception {
        String victim = "autista7@demo.it";
        for (int attempt = 0; attempt < 10; attempt++) {
            remoteAddress = "10.99.1." + (attempt + 1);
            Actor start = anonymous();
            perform(start, post("/api/v1/auth/login"), "{\"email\":\"" + victim + "\",\"password\":\"x\"}")
                    .andExpect(status().isUnauthorized());
        }
        remoteAddress = "10.99.2.1";
        Actor start = anonymous();
        perform(start, post("/api/v1/auth/login"), "{\"email\":\"" + victim.toUpperCase() + "\",\"password\":\"x\"}")
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void oversizedCredentialsAreRejectedBeforeHashing() throws Exception {
        Actor start = anonymous();
        perform(start, post("/api/v1/auth/login"),
                "{\"email\":\"" + RESPONSABILE + "\",\"password\":\"" + "x".repeat(129) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void csrfTokenRotatesAtLoginAndOldTokenStopsWorking() throws Exception {
        Actor start = anonymous();
        String sessionBefore = start.session().getId();
        perform(start, post("/api/v1/auth/login"),
                "{\"email\":\"" + RESPONSABILE + "\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isOk());
        assertThat(start.session().getId()).isNotEqualTo(sessionBefore);
        perform(start, post("/api/v1/auth/logout")).andExpect(status().isForbidden());
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        Actor manager = login(RESPONSABILE);
        perform(manager, post("/api/v1/auth/logout")).andExpect(status().isNoContent());
        assertThat(manager.session().isInvalid()).isTrue();
        mvc.perform(get("/api/v1/auth/me").session(new MockHttpSession())).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({
        "GET,/api/v1/autisti", "GET,/api/v1/mezzi", "GET,/api/v1/compagnie", "GET,/api/v1/clienti",
        "GET,/api/v1/officine", "GET,/api/v1/missioni", "GET,/api/v1/dashboard", "GET,/api/v1/centrale/avvisi",
        "GET,/api/v1/missioni/1/proposte", "POST,/api/v1/clienti", "POST,/api/v1/missioni",
        "POST,/api/v1/missioni/1/proposte", "POST,/api/v1/missioni/1/proposta-automatica",
        "PATCH,/api/v1/missioni/1/intervento", "PATCH,/api/v1/missioni/1/cost"})
    void managerEndpointsRefuseAnonymousAndDrivers(String method, String path) throws Exception {
        HttpMethod verb = HttpMethod.valueOf(method);
        Actor anonymous = anonymous();
        perform(anonymous, MockMvcRequestBuilders.request(verb, path), "{}").andExpect(status().isUnauthorized());
        perform(login(AUTISTA_1), MockMvcRequestBuilders.request(verb, path), "{}").andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/v1/autista/missioni", "GET,/api/v1/autista/storico", "GET,/api/v1/autista/missioni/1",
        "POST,/api/v1/missioni/1/accept", "POST,/api/v1/missioni/1/reject", "POST,/api/v1/missioni/1/complete"})
    void driverEndpointsRefuseAnonymousAndManagers(String method, String path) throws Exception {
        HttpMethod verb = HttpMethod.valueOf(method);
        perform(anonymous(), MockMvcRequestBuilders.request(verb, path), "{}").andExpect(status().isUnauthorized());
        perform(login(RESPONSABILE), MockMvcRequestBuilders.request(verb, path), "{}").andExpect(status().isForbidden());
    }

    @Test
    void mutationsWithoutCsrfAreRefusedEvenWithASession() throws Exception {
        Actor manager = login(RESPONSABILE);
        mvc.perform(post("/api/v1/missioni").session(manager.session()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/maps/route").param("missionId", "1").session(manager.session()))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherDriverCannotReachAMission() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        propose(manager, mission, 1);
        Actor other = login(AUTISTA_2);
        perform(other, get("/api/v1/missioni/{id}", mission)).andExpect(status().isForbidden());
        perform(other, get("/api/v1/autista/missioni/{id}", mission)).andExpect(status().isForbidden());
        perform(other, get("/api/v1/missioni/{id}/notes", mission)).andExpect(status().isForbidden());
        perform(other, post("/api/v1/missioni/{id}/notes", mission), "{\"testo\":\"x\"}").andExpect(status().isForbidden());
        perform(other, get("/api/v1/maps/route").param("missionId", String.valueOf(mission))).andExpect(status().isForbidden());
        perform(other, post("/api/v1/missioni/{id}/accept", mission)).andExpect(status().isForbidden());
        perform(other, post("/api/v1/missioni/{id}/reject", mission)).andExpect(status().isForbidden());
        perform(other, post("/api/v1/missioni/{id}/complete", mission), "{\"kmEffettivi\":1}").andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select stato from missions where id = ?", String.class, mission)).isEqualTo("proposta");
    }

    @Test
    void aFormerRecipientLosesAccessAfterRejecting() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        propose(manager, mission, 1);
        Actor driver = login(AUTISTA_1);
        perform(driver, post("/api/v1/missioni/{id}/reject", mission)).andExpect(status().isOk());
        perform(driver, get("/api/v1/missioni/{id}", mission)).andExpect(status().isForbidden());
        perform(driver, get("/api/v1/maps/route").param("missionId", String.valueOf(mission))).andExpect(status().isForbidden());
    }

    @Test
    void driversCannotReadHistoricalFixtures() throws Exception {
        perform(login(AUTISTA_1), get("/api/v1/missioni/{id}", fixtureMission())).andExpect(status().isForbidden());
    }

    @Test
    void contactsAreVisibleFromTheProposalAndThePlateAfterAcceptance() throws Exception {
        String phone = jdbc.queryForObject("select telefono from customers where id = 1", String.class);
        String name = jdbc.queryForObject("select nome from customers where id = 1", String.class);
        String plate = jdbc.queryForObject("select targa from customers where id = 1", String.class);
        String policy = jdbc.queryForObject("select numero_polizza from customers where id = 1", String.class);
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        propose(manager, mission, 1);
        Actor driver = login(AUTISTA_1);

        List<String> bodies = new ArrayList<>();
        bodies.add(perform(driver, get("/api/v1/autista/missioni")).andReturn().getResponse().getContentAsString());
        bodies.add(perform(driver, get("/api/v1/autista/missioni/{id}", mission)).andReturn().getResponse().getContentAsString());
        bodies.add(perform(driver, get("/api/v1/missioni/{id}", mission)).andReturn().getResponse().getContentAsString());
        assertThat(bodies).allSatisfy(body -> assertThat(body).contains(phone, name).doesNotContain(plate, policy));

        String accepted = perform(driver, post("/api/v1/missioni/{id}/accept", mission))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(accepted).contains(phone, name).doesNotContain(policy);
        String detail = perform(driver, get("/api/v1/autista/missioni/{id}", mission))
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).contains(phone, name, plate).doesNotContain(policy);

        perform(driver, post("/api/v1/missioni/{id}/complete", mission), "{\"kmEffettivi\":10}").andExpect(status().isOk());
        String history = perform(driver, get("/api/v1/autista/storico")).andReturn().getResponse().getContentAsString();
        assertThat(history).doesNotContain(phone, name, plate, policy);
    }

    @Test
    void driverCannotAddNotesBeforeAccepting() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        propose(manager, mission, 1);
        Actor driver = login(AUTISTA_1);
        perform(driver, post("/api/v1/missioni/{id}/notes", mission), "{\"testo\":\"prima\"}").andExpect(status().isConflict());
        perform(driver, post("/api/v1/missioni/{id}/accept", mission)).andExpect(status().isOk());
        perform(driver, post("/api/v1/missioni/{id}/notes", mission), "{\"testo\":\"dopo\"}").andExpect(status().isCreated());
        perform(manager, get("/api/v1/missioni/{id}/notes", mission)).andExpect(jsonPath("$[0].testo").value("dopo"));
    }

    @Test
    void costCanOnlyBeCorrectedByTheManager() throws Exception {
        perform(login(AUTISTA_1), patch("/api/v1/missioni/{id}/cost", 1), "{\"costoFinale\":1,\"motivo\":\"abc\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void customerSearchTreatsInputAsText() throws Exception {
        Actor manager = login(RESPONSABILE);
        perform(manager, get("/api/v1/clienti").param("search", "' OR 1=1 --")).andExpect(jsonPath("$.length()").value(0));
        perform(manager, get("/api/v1/clienti").param("search", "%")).andExpect(jsonPath("$.length()").value(0));
        perform(manager, get("/api/v1/clienti").param("search", "_")).andExpect(jsonPath("$.length()").value(0));
        perform(manager, get("/api/v1/clienti").param("search", "x".repeat(101))).andExpect(status().isBadRequest());
    }

    @Test
    void customerInputIsValidated() throws Exception {
        Actor manager = login(RESPONSABILE);
        String valid = "{\"nome\":\"Prova\",\"telefono\":\"+39 333 1234567\",\"targa\":\"%s\",\"compagniaId\":1,"
                + "\"numeroPolizza\":\"P1\",\"scadenzaPolizza\":\"2030-01-01\"}";
        perform(manager, post("/api/v1/clienti"), valid.formatted("zz 999 zz")).andExpect(status().isCreated());
        perform(manager, post("/api/v1/clienti"), valid.formatted("ZZ999ZZ"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Targa già presente"));
        perform(manager, post("/api/v1/clienti"), valid.formatted("AB")).andExpect(status().isBadRequest());
        perform(manager, post("/api/v1/clienti"), valid.formatted("YY888YY").replace("+39 333 1234567", "<script>"))
                .andExpect(status().isBadRequest());
    }
}

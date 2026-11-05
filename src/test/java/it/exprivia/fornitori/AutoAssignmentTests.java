package it.exprivia.fornitori;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.exprivia.fornitori.service.AutoAssignmentService;
import it.exprivia.fornitori.service.MissionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/** Assegnazione automatica attiva: l'unica classe di test in cui parte da sola, su un altro thread. */
@EnabledIfEnvironmentVariable(named = "FORNITORI_TEST_DATABASE", matches = "isolated")
@TestPropertySource(properties = "fornitori.missions.auto-assign=true")
class AutoAssignmentTests extends IsolatedPostgresTest {
    @Autowired MissionService missions;
    @Autowired AutoAssignmentService assignments;

    @Test
    void creationProposesToTheNearestAvailableDriver() throws Exception {
        long nearest = driverStartingAt(workshopOf(4));
        long mission = createMissionAt(login(RESPONSABILE), workshopOf(4));
        awaitDriver(mission, nearest);
        assertThat(state(mission)).isEqualTo("proposta");
        assertThat(jdbc.queryForObject("select detail ->> 'automatica' from mission_events "
                + "where mission_id = ? and event_type = 'MISSION_PROPOSED'", String.class, mission)).isEqualTo("true");
    }

    @Test
    void busyAndUnavailableDriversAreSkipped() throws Exception {
        long workshop = workshopOf(4);
        long nearest = driverStartingAt(workshop);
        perform(login(emailOf(nearest)), patch("/api/v1/autista/disponibilita"), "{\"disponibile\":false}")
                .andExpect(status().isOk());
        Actor manager = login(RESPONSABILE);
        long first = createMissionAt(manager, workshop);
        long second = awaitProposedDriver(first);
        assertThat(second).isNotEqualTo(nearest);

        long other = createMissionAt(manager, workshop);
        long third = awaitProposedDriver(other);
        assertThat(third).isNotIn(nearest, second);
    }

    @Test
    void aRejectionMovesToTheNextDriverAndTheThirdHandsOverToTheCentre() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMissionAt(manager, workshopOf(4));
        long first = awaitProposedDriver(mission);
        reject(first, mission);
        long second = awaitNewDriver(mission, List.of(first));
        reject(second, mission);
        long third = awaitNewDriver(mission, List.of(first, second));
        perform(login(emailOf(third)), post("/api/v1/missioni/{id}/reject", mission))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("gestione_manuale"))
                .andExpect(jsonPath("$.esitiNegativi").value(3));

        assignments.retryPending();
        assertThat(state(mission)).isEqualTo("gestione_manuale");
        assertThat(jdbc.queryForObject("select count(*) from mission_proposals where mission_id = ?", Integer.class, mission))
                .isEqualTo(3);

        perform(manager, post("/api/v1/missioni/{id}/proposta-automatica", mission)).andExpect(status().isOk());
        assertThat(state(mission)).isEqualTo("proposta");
        assertThat(jdbc.queryForObject("select count(distinct driver_id) from mission_proposals where mission_id = ?",
                Integer.class, mission)).isEqualTo(4);
    }

    @Test
    void anExpiredProposalMovesToTheNextDriver() throws Exception {
        long mission = createMissionAt(login(RESPONSABILE), workshopOf(4));
        long first = awaitProposedDriver(mission);
        expireProposal(mission);
        missions.expireDueProposals();
        assertThat(awaitNewDriver(mission, List.of(first))).isNotEqualTo(first);
    }

    @Test
    void theSchedulerRetriesWhenADriverBecomesAvailable() throws Exception {
        jdbc.update("update drivers set disponibile = false");
        long mission = createMissionAt(login(RESPONSABILE), workshopOf(4));
        assignments.retryPending();
        assertThat(state(mission)).isEqualTo("nuova");
        jdbc.update("update drivers set disponibile = true");
        assignments.retryPending();
        assertThat(state(mission)).isEqualTo("proposta");
    }

    @Test
    void aConfirmedPolicyStartsTheAssignment() throws Exception {
        String session = session();
        chat(post("/api/v1/chat/lookup-targa"), lookupBody(session, "ZZ999ZZ"))
                .andExpect(jsonPath("$.esito").value("DA_VERIFICARE"));
        String body = "{\"sessionId\":\"" + session + "\",\"requestId\":\"" + session() + "\",\"targa\":\"ZZ999ZZ\","
                + "\"telefonoSulPosto\":\"333 1234567\",\"tipoProblema\":\"BATTERIA_SCARICA\","
                + "\"origineIndirizzo\":\"Via Roma 1, Molfetta\",\"origineLat\":41.2,\"origineLng\":16.6,"
                + "\"canaleOrigine\":\"telegram\",\"destinatario\":\"chat-1\"}";
        long mission = read(chat(post("/api/v1/chat/missioni"), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stato").value("verifica_polizza"))
                .andExpect(jsonPath("$.inVerifica").value(true))
                .andReturn()).get("id").asLong();

        assignments.retryPending();
        assertThat(state(mission)).isEqualTo("verifica_polizza");
        perform(login(RESPONSABILE), post("/api/v1/missioni/{id}/verifica-polizza", mission),
                "{\"esito\":\"CONFERMATA\",\"clienteId\":1}").andExpect(status().isOk());
        awaitProposedDriver(mission);
        assertThat(state(mission)).isEqualTo("proposta");
    }

    private long createMissionAt(Actor manager, long workshop) throws Exception {
        var point = jdbc.queryForMap("select lat, lng from workshops where id = ?", workshop);
        String body = "{\"clienteId\":1,\"tipoIntervento\":\"riparazione_sul_posto\",\"descrizioneProblema\":\"Non parte\","
                + "\"origineIndirizzo\":\"Via Roma 1\",\"origineLat\":" + point.get("lat") + ",\"origineLng\":" + point.get("lng")
                + ",\"canaleOrigine\":\"centrale\"}";
        return read(perform(manager, post("/api/v1/missioni"), body).andExpect(status().isCreated()).andReturn())
                .get("id").asLong();
    }

    private void reject(long driver, long mission) throws Exception {
        perform(login(emailOf(driver)), post("/api/v1/missioni/{id}/reject", mission)).andExpect(status().isOk());
    }

    private long workshopOf(long driver) {
        return jdbc.queryForObject("select officina_riferimento_id from drivers where id = ?", Long.class, driver);
    }

    private long driverStartingAt(long workshop) {
        return jdbc.queryForObject("select min(id) from drivers where officina_riferimento_id = ?", Long.class, workshop);
    }

    private String emailOf(long driver) {
        return jdbc.queryForObject("select u.email from app_users u join drivers d on d.user_id = u.id where d.id = ?",
                String.class, driver);
    }

    private String state(long mission) {
        return jdbc.queryForObject("select stato from missions where id = ?", String.class, mission);
    }

    private void awaitDriver(long mission, long driver) throws Exception {
        assertThat(awaitProposedDriver(mission)).isEqualTo(driver);
    }

    private long awaitProposedDriver(long mission) throws Exception {
        return awaitNewDriver(mission, List.of());
    }

    /** L'assegnazione gira dopo il commit su un altro thread: qui si aspetta la proposta a un autista non ancora visto. */
    private long awaitNewDriver(long mission, List<Long> seen) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            List<Long> open = jdbc.queryForList("select driver_id from missions where id = ? and stato = 'proposta'",
                    Long.class, mission);
            if (!open.isEmpty() && !seen.contains(open.get(0))) {
                return open.get(0);
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Nessuna proposta automatica per la missione " + mission);
    }
}

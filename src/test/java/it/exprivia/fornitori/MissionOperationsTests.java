package it.exprivia.fornitori;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.exprivia.fornitori.service.MapProviders;
import it.exprivia.fornitori.service.MissionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/** Tre esiti negativi, annullamenti e compensi, km con ritorno, supporto, disponibilità e dashboard operativa. */
@EnabledIfEnvironmentVariable(named = "FORNITORI_TEST_DATABASE", matches = "isolated")
class MissionOperationsTests extends IsolatedPostgresTest {
    private static final String AUTISTA_3 = "autista3@demo.it";
    private static final Map<String, Object> LINE = Map.of("type", "LineString", "coordinates", List.of());

    @Autowired MissionService missions;

    @Test
    void threeDistinctNegativeOutcomesRequireManualHandling() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        propose(manager, mission, 1);
        perform(login(AUTISTA_1), post("/api/v1/missioni/{id}/reject", mission))
                .andExpect(jsonPath("$.stato").value("non_accettata")).andExpect(jsonPath("$.esitiNegativi").value(1));
        propose(manager, mission, 2);
        expireProposal(mission);
        missions.expireDueProposals();
        assertThat(state(mission)).isEqualTo("scaduta");
        propose(manager, mission, 3);
        perform(login(AUTISTA_3), post("/api/v1/missioni/{id}/reject", mission))
                .andExpect(jsonPath("$.stato").value("gestione_manuale")).andExpect(jsonPath("$.esitiNegativi").value(3));
        assertThat(jdbc.queryForObject("select detail ->> 'motivo' from mission_events where mission_id = ? "
                + "and event_type = 'MISSION_MANUAL_HANDLING'", String.class, mission)).isEqualTo("TRE_RIFIUTI");

        // La centrale può ancora proporre; un altro rifiuto riporta la missione a lei, non in riassegnazione.
        propose(manager, mission, 4);
        perform(login("autista4@demo.it"), post("/api/v1/missioni/{id}/reject", mission))
                .andExpect(jsonPath("$.stato").value("gestione_manuale")).andExpect(jsonPath("$.esitiNegativi").value(4));
    }

    @Test
    void theSameDriverRefusingTwiceCountsOnce() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        Actor driver = login(AUTISTA_1);
        for (int refusal = 0; refusal < 3; refusal++) {
            propose(manager, mission, 1);
            perform(driver, post("/api/v1/missioni/{id}/reject", mission))
                    .andExpect(jsonPath("$.stato").value("non_accettata")).andExpect(jsonPath("$.esitiNegativi").value(1));
        }
    }

    @Test
    void aDriverCancellingEarnsNothingAndKeepsTheMissionInHistory() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        Actor driver = login(AUTISTA_1);
        Actor other = login(AUTISTA_2);
        perform(driver, post("/api/v1/missioni/{id}/annulla", mission), "{\"motivo\":\"Guasto al carro\"}")
                .andExpect(status().isForbidden());
        propose(manager, mission, 1);
        perform(driver, post("/api/v1/missioni/{id}/annulla", mission), "{\"motivo\":\"Guasto al carro\"}")
                .andExpect(status().isConflict());
        perform(driver, post("/api/v1/missioni/{id}/accept", mission)).andExpect(status().isOk());
        perform(other, post("/api/v1/missioni/{id}/annulla", mission), "{\"motivo\":\"Guasto al carro\"}")
                .andExpect(status().isForbidden());
        perform(driver, post("/api/v1/missioni/{id}/annulla", mission), "{\"motivo\":\"  x \"}").andExpect(status().isBadRequest());

        perform(driver, post("/api/v1/missioni/{id}/annulla", mission), "{\"motivo\":\"Guasto al carro\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("annullata"))
                .andExpect(jsonPath("$.annullataDa").value("autista"))
                .andExpect(jsonPath("$.autistaId").value(1))
                .andExpect(jsonPath("$.costoFinale").value(0))
                .andExpect(jsonPath("$.costoCalcolato").value(0))
                .andExpect(jsonPath("$.motivoAnnullamento").value("Guasto al carro"));
        verify(routing, never()).route(any());
        perform(driver, post("/api/v1/missioni/{id}/annulla", mission), "{\"motivo\":\"Di nuovo\"}").andExpect(status().isConflict());
        perform(manager, patch("/api/v1/missioni/{id}/cost", mission), "{\"costoFinale\":10,\"motivo\":\"prova\"}")
                .andExpect(status().isConflict());
        perform(driver, get("/api/v1/autista/storico"))
                .andExpect(jsonPath("$.missioni[0].id").value(mission))
                .andExpect(jsonPath("$.missioni[0].clienteNome").value(nullValue()))
                .andExpect(jsonPath("$.totaleEur").value(0));
        perform(manager, get("/api/v1/dashboard"))
                .andExpect(jsonPath("$.totaleEur").value(0))
                .andExpect(jsonPath("$.totalePerAutista.length()").value(0))
                .andExpect(jsonPath("$.categorie.annullate").value(1));
    }

    @Test
    void theCentreCancellingBeforeAcceptanceChargesNobody() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        propose(manager, mission, 1);
        perform(login(AUTISTA_1), post("/api/v1/missioni/{id}/annulla", mission), "{\"motivo\":\"Non posso\"}")
                .andExpect(status().isConflict());
        perform(manager, post("/api/v1/missioni/{id}/annulla", mission), "{\"motivo\":\"Cliente risolto\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annullataDa").value("centrale"))
                .andExpect(jsonPath("$.autistaId").value(nullValue()))
                .andExpect(jsonPath("$.costoFinale").value(0));
        assertThat(jdbc.queryForObject("select stato from mission_proposals where mission_id = ?", String.class, mission))
                .isEqualTo("chiusa");
        propose(manager, createMission(manager), 1);
    }

    @Test
    void theCentreCancellingAfterAcceptanceChargesTheDriverEverywhere() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        propose(manager, mission, 1);
        Actor driver = login(AUTISTA_1);
        perform(driver, post("/api/v1/missioni/{id}/accept", mission)).andExpect(status().isOk());
        jdbc.update("update missions set km_stimati = 10, accettata_il = "
                + "(timestamp '2026-11-04 12:00:00' at time zone 'Europe/Rome') where id = ?", mission);

        perform(manager, post("/api/v1/missioni/{id}/annulla", mission), "{\"motivo\":\"Doppione\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autistaId").value(1))
                .andExpect(jsonPath("$.costoFinale").value(62.00));
        perform(driver, get("/api/v1/autista/storico")).andExpect(jsonPath("$.totaleEur").value(62.00));
        perform(manager, get("/api/v1/dashboard"))
                .andExpect(jsonPath("$.totaleEur").value(62.00))
                .andExpect(jsonPath("$.totalePerAutista[0].totaleEur").value(62.00))
                .andExpect(jsonPath("$.totalePerAutista[0].missioniEseguite").value(0))
                .andExpect(jsonPath("$.costi.totaleEseguite").value(0))
                .andExpect(jsonPath("$.costi.totaleAnnullateAddebitate").value(62.00))
                .andExpect(jsonPath("$.costi.totaleGenerale").value(62.00))
                .andExpect(jsonPath("$.costi.perAutista[0].missioni").value(1))
                .andExpect(jsonPath("$.costi.perMese.length()").value(1));

        perform(manager, patch("/api/v1/missioni/{id}/cost", mission), "{\"costoFinale\":30,\"motivo\":\"abbuono\"}")
                .andExpect(jsonPath("$.costoFinale").value(30))
                .andExpect(jsonPath("$.costoCalcolato").value(62.00));
        perform(driver, get("/api/v1/autista/storico")).andExpect(jsonPath("$.totaleEur").value(30));
        perform(manager, get("/api/v1/dashboard")).andExpect(jsonPath("$.totaleEur").value(30));
    }

    @Test
    void missingKmAreEstimatedWithTheReturnAndSurviveARoutingOutage() throws Exception {
        Actor manager = login(RESPONSABILE);
        long first = acceptedMission(manager, 1, AUTISTA_1);
        when(routing.route(any())).thenReturn(new MapProviders.RouteResult(new BigDecimal("20.00"), LINE));
        perform(manager, post("/api/v1/missioni/{id}/annulla", first), "{\"motivo\":\"Doppione\"}")
                .andExpect(jsonPath("$.kmStimati").value(20.00))
                .andExpect(jsonPath("$.costoCalcolato").isNumber());

        long second = acceptedMission(manager, 2, AUTISTA_2);
        when(routing.route(any())).thenThrow(new MapProviders.ProviderUnavailableException("giù"));
        JsonNode cancelled = read(perform(manager, post("/api/v1/missioni/{id}/annulla", second), "{\"motivo\":\"Doppione\"}")
                .andExpect(status().isOk()).andReturn());
        assertThat(cancelled.get("kmStimati").asDouble()).isGreaterThan(0);
    }

    @Test
    void theRouteComesBackToTheStartAndSplitsTheKm() throws Exception {
        Actor manager = login(RESPONSABILE);
        long mission = createMission(manager);
        propose(manager, mission, 1);
        when(routing.route(any())).thenReturn(new MapProviders.RouteResult(new BigDecimal("30.01"), LINE,
                List.of(new BigDecimal("10.00"), new BigDecimal("20.00"))));
        perform(manager, get("/api/v1/maps/route").param("missionId", String.valueOf(mission)).param("autistaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kmAndata").value(10.00))
                .andExpect(jsonPath("$.kmTraino").value(0))
                .andExpect(jsonPath("$.kmRitorno").value(20.00))
                .andExpect(jsonPath("$.kmTotali").value(30.00))
                .andExpect(jsonPath("$.km").value(30.00))
                .andExpect(jsonPath("$.kmStimati").value(30.00))
                .andExpect(jsonPath("$.punti[0].tipo").value("base"))
                .andExpect(jsonPath("$.punti.length()").value(2));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MapProviders.GeoPoint>> trip = ArgumentCaptor.forClass(List.class);
        verify(routing).route(trip.capture());
        assertThat(trip.getValue()).hasSize(3);
        assertThat(trip.getValue().get(2)).isEqualTo(trip.getValue().get(0));
        assertThat(jdbc.queryForObject("select km_stimati from missions where id = ?", BigDecimal.class, mission))
                .isEqualByComparingTo("30.00");
        BigDecimal workshopLat = jdbc.queryForObject("select w.lat from workshops w join drivers d "
                + "on d.officina_riferimento_id = w.id where d.id = 1", BigDecimal.class);
        assertThat(trip.getValue().get(0).lat()).isEqualByComparingTo(workshopLat);
    }

    @Test
    void towingAddsTheWorkshopLeg() throws Exception {
        Actor manager = login(RESPONSABILE);
        String body = "{\"clienteId\":1,\"tipoIntervento\":\"traino\",\"officinaId\":1,\"descrizioneProblema\":\"Motore\","
                + "\"origineIndirizzo\":\"Via Roma 1\",\"origineLat\":41.2,\"origineLng\":16.6,\"canaleOrigine\":\"centrale\"}";
        long mission = read(perform(manager, post("/api/v1/missioni"), body).andExpect(status().isCreated()).andReturn())
                .get("id").asLong();
        propose(manager, mission, 1);
        when(routing.route(any())).thenReturn(new MapProviders.RouteResult(new BigDecimal("6"), LINE,
                List.of(new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00"))));
        perform(manager, get("/api/v1/maps/route").param("missionId", String.valueOf(mission)))
                .andExpect(jsonPath("$.kmAndata").value(1.00))
                .andExpect(jsonPath("$.kmTraino").value(2.00))
                .andExpect(jsonPath("$.kmRitorno").value(3.00))
                .andExpect(jsonPath("$.kmTotali").value(6.00))
                .andExpect(jsonPath("$.punti[2].tipo").value("officina"));
    }

    @Test
    void anOpenSupportRequestReachesTheQueueAndTheAlertsUntilHandled() throws Exception {
        Actor manager = login(RESPONSABILE);
        long rejected = createMission(manager);
        propose(manager, rejected, 2);
        perform(login(AUTISTA_2), post("/api/v1/missioni/{id}/reject", rejected)).andExpect(status().isOk());

        long mission = createMission(manager);
        propose(manager, mission, 1);
        Actor driver = login(AUTISTA_1);
        perform(driver, post("/api/v1/missioni/{id}/notes", mission), "{\"testo\":\"Aiuto\",\"tipo\":\"SUPPORTO\"}")
                .andExpect(status().isConflict());
        perform(driver, post("/api/v1/missioni/{id}/accept", mission)).andExpect(status().isOk());
        perform(manager, post("/api/v1/missioni/{id}/notes", mission), "{\"testo\":\"Aiuto\",\"tipo\":\"SUPPORTO\"}")
                .andExpect(status().isForbidden());
        perform(driver, post("/api/v1/missioni/{id}/notes", mission), "{\"testo\":\"Aiuto\",\"tipo\":\"ALTRO\"}")
                .andExpect(status().isBadRequest());
        perform(driver, post("/api/v1/missioni/{id}/notes", mission), "{\"testo\":\"Solo un avviso\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipo").value("AVVISO"));
        long note = read(perform(driver, post("/api/v1/missioni/{id}/notes", mission), "{\"testo\":\"Aiuto\",\"tipo\":\"SUPPORTO\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gestitaIl").value(nullValue()))
                .andReturn()).get("id").asLong();

        perform(manager, get("/api/v1/dashboard"))
                .andExpect(jsonPath("$.categorie.supportoAperto").value(1))
                .andExpect(jsonPath("$.categorie.critiche").value(1))
                .andExpect(jsonPath("$.codaCritica[0].id").value(mission))
                .andExpect(jsonPath("$.codaCritica[0].motivo").value("SUPPORTO_AUTISTA"));
        perform(manager, get("/api/v1/missioni/{id}", mission)).andExpect(jsonPath("$.supportoAperto").value(true));
        perform(manager, get("/api/v1/centrale/avvisi"))
                .andExpect(jsonPath("$[0].tipo").value("SUPPORTO"))
                .andExpect(jsonPath("$[0].noteId").value(note))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].testo").value("Aiuto"))
                .andExpect(jsonPath("$[1].tipo").value("RIFIUTO"));

        perform(driver, post("/api/v1/missioni/{id}/notes/{note}/gestita", mission, note)).andExpect(status().isForbidden());
        perform(manager, post("/api/v1/missioni/{id}/notes/{note}/gestita", rejected, note)).andExpect(status().isNotFound());
        perform(manager, post("/api/v1/missioni/{id}/notes/{note}/gestita", mission, note))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gestitaIl").isNotEmpty());
        perform(manager, post("/api/v1/missioni/{id}/notes/{note}/gestita", mission, note)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from mission_events where mission_id = ? "
                + "and event_type = 'MISSION_SUPPORT_HANDLED'", Integer.class, mission)).isEqualTo(1);
        perform(manager, get("/api/v1/dashboard"))
                .andExpect(jsonPath("$.categorie.critiche").value(0))
                .andExpect(jsonPath("$.codaCritica.length()").value(0));
        perform(manager, get("/api/v1/centrale/avvisi")).andExpect(jsonPath("$[0].tipo").value("RIFIUTO"));
    }

    @Test
    void anUnavailableDriverReceivesNoProposal() throws Exception {
        Actor driver = login(AUTISTA_1);
        perform(driver, get("/api/v1/auth/me")).andExpect(jsonPath("$.disponibile").value(true));
        perform(login(RESPONSABILE), patch("/api/v1/autista/disponibilita"), "{\"disponibile\":false}")
                .andExpect(status().isForbidden());
        perform(driver, patch("/api/v1/autista/disponibilita"), "{}").andExpect(status().isBadRequest());
        perform(driver, patch("/api/v1/autista/disponibilita"), "{\"disponibile\":false}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponibile").value(false));
        perform(driver, get("/api/v1/auth/me")).andExpect(jsonPath("$.disponibile").value(false));

        Actor manager = login(RESPONSABILE);
        perform(manager, get("/api/v1/auth/me")).andExpect(jsonPath("$.disponibile").value(nullValue()));
        perform(manager, get("/api/v1/autisti")).andExpect(jsonPath("$[0].disponibilita").value("non_disponibile"));
        perform(manager, post("/api/v1/missioni/{id}/proposte", createMission(manager)), "{\"autistaId\":1}")
                .andExpect(status().isConflict());
    }

    @Test
    void theDashboardCountsEveryCategoryAndKeepsTheReferenceBaseline() throws Exception {
        Actor manager = login(RESPONSABILE);
        createMission(manager);
        long proposed = createMission(manager);
        propose(manager, proposed, 1);
        long rejected = createMission(manager);
        propose(manager, rejected, 2);
        perform(login(AUTISTA_2), post("/api/v1/missioni/{id}/reject", rejected)).andExpect(status().isOk());

        perform(manager, get("/api/v1/dashboard"))
                .andExpect(jsonPath("$.pending").value(2))
                .andExpect(jsonPath("$.categorie.daAssegnare").value(1))
                .andExpect(jsonPath("$.categorie.pending").value(1))
                .andExpect(jsonPath("$.categorie.ko").value(1))
                .andExpect(jsonPath("$.categorie.ok").value(0))
                .andExpect(jsonPath("$.distribuzioneStati.nuova").value(1))
                .andExpect(jsonPath("$.distribuzioneStati.annullata").value(0))
                .andExpect(jsonPath("$.distribuzioneStati.verifica_polizza").value(0));
        perform(manager, get("/api/v1/dashboard").param("mode", "reference"))
                .andExpect(jsonPath("$.eseguite").value(16))
                .andExpect(jsonPath("$.totaleEur").value(1509.71))
                .andExpect(jsonPath("$.costi.totaleEseguite").value(1509.71))
                .andExpect(jsonPath("$.categorie.ok").value(16))
                .andExpect(jsonPath("$.fonte").value("excel_demo"));
    }

    @Test
    void theDriverSeesContactsFromTheProposalAndThePlateAfterAccepting() throws Exception {
        Actor manager = login(RESPONSABILE);
        jdbc.update("update customers set cognome = 'Prova' where id = 1");
        try {
            String phone = jdbc.queryForObject("select telefono from customers where id = 1", String.class);
            long mission = createMission(manager);
            propose(manager, mission, 1);
            Actor driver = login(AUTISTA_1);
            perform(driver, get("/api/v1/autista/missioni/{id}", mission))
                    .andExpect(jsonPath("$.cliente.cognome").value("Prova"))
                    .andExpect(jsonPath("$.cliente.telefono").value(phone))
                    .andExpect(jsonPath("$.cliente.targa").value(nullValue()))
                    .andExpect(jsonPath("$.missione.clienteCognome").value("Prova"));
            perform(driver, get("/api/v1/autista/missioni")).andExpect(jsonPath("$[0].clienteTelefono").value(phone));
            perform(driver, post("/api/v1/missioni/{id}/accept", mission)).andExpect(status().isOk());
            perform(driver, get("/api/v1/autista/missioni/{id}", mission)).andExpect(jsonPath("$.cliente.targa").isNotEmpty());
        } finally {
            jdbc.update("update customers set cognome = null where id = 1");
        }
    }

    @Test
    void customersHaveAnOptionalSurname() throws Exception {
        Actor manager = login(RESPONSABILE);
        String body = "{\"nome\":\"Anna\",\"cognome\":\"Rossi\",\"telefono\":\"3331234567\",\"targa\":\"AA111AA\","
                + "\"compagniaId\":1,\"numeroPolizza\":\"P-1\",\"scadenzaPolizza\":\"2099-01-01\"}";
        perform(manager, post("/api/v1/clienti"), body)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.cognome").value("Rossi"));
        perform(manager, get("/api/v1/clienti").param("search", "Rossi")).andExpect(jsonPath("$[0].nome").value("Anna"));
    }

    private long acceptedMission(Actor manager, long driver, String email) throws Exception {
        long mission = createMission(manager);
        propose(manager, mission, driver);
        perform(login(email), post("/api/v1/missioni/{id}/accept", mission)).andExpect(status().isOk());
        return mission;
    }

    private String state(long mission) {
        return jdbc.queryForObject("select stato from missions where id = ?", String.class, mission);
    }
}

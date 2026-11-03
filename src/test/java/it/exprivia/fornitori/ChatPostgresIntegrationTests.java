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
import it.exprivia.fornitori.service.MissionService;
import java.math.BigDecimal;
import java.util.Map;
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
    private static final String POLICY = "PS-2026-730226";
    private static final String OTHER_PLATE = "ZS795LG";
    private static final String OTHER_POLICY = "AD-2026-571497";
    private static final String RESPONSABILE = "responsabile@demo.it";
    private static final String AUTISTA = "autista1@demo.it";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MissionService missions;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpServletRequestBuilder asBot(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer token-di-prova");
    }

    private ResultActions lookup(String plate, String policy) throws Exception {
        return mockMvc.perform(asBot(post("/api/v1/chat/lookup-targa"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sessionId":"%s","targa":"%s","verificationChallenge":"%s"}
                        """.formatted(SESSION, plate, policy)));
    }

    private String verify(String plate, String policy) throws Exception {
        String body = lookup(plate, policy).andExpect(status().isOk())
                .andExpect(jsonPath("$.verificato").value(true))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.verificationToken");
    }

    private ResultActions create(String token, String requestId, long customerId) throws Exception {
        return mockMvc.perform(asBot(post("/api/v1/chat/missioni"))
                .header("X-Verification-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sessionId":"%s","requestId":"%s","clienteId":%d,
                         "tipoIntervento":"riparazione_sul_posto","descrizioneProblema":"Motore in panne",
                         "origineIndirizzo":"Via Roma 1, Molfetta","origineLat":41.2,"origineLng":16.6,
                         "officinaId":null,"canaleOrigine":"telegram"}
                        """.formatted(SESSION, requestId, customerId)));
    }

    private long createdId(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions readStatus(String token, long missionId) throws Exception {
        return mockMvc.perform(asBot(get("/api/v1/chat/missioni/{id}/stato", missionId))
                .header("X-Verification-Token", token));
    }

    @Test
    void botFollowsMissionFromCreationToCompletion() throws Exception {
        String token = verify(PLATE, POLICY);
        long missionId = createdId(create(token, "richiesta-di-prova-01", 1L)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stato").value("nuova")));
        assertEquals("telegram", missionRepository.findById(missionId).orElseThrow().getCanaleOrigine());

        readStatus(token, missionId).andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("nuova"))
                .andExpect(jsonPath("$.autistaNome").isEmpty());

        missions.propose(RESPONSABILE, missionId, new MissionDtos.ProposeMissionRequest(1L));
        readStatus(token, missionId).andExpect(jsonPath("$.stato").value("proposta"))
                .andExpect(jsonPath("$.autistaNome").isEmpty());

        missions.accept(AUTISTA, missionId);
        readStatus(token, missionId).andExpect(jsonPath("$.stato").value("accettata"))
                .andExpect(jsonPath("$.autistaNome").value("Luca Bianchi"));

        missions.complete(AUTISTA, missionId, new MissionDtos.CompleteMissionRequest(new BigDecimal("10.00")));
        readStatus(token, missionId).andExpect(jsonPath("$.stato").value("eseguita"))
                .andExpect(jsonPath("$.autistaNome").value("Luca Bianchi"));
    }

    @Test
    void sameRequestIdReturnsSameMission() throws Exception {
        String token = verify(PLATE, POLICY);
        long first = createdId(create(token, "richiesta-di-prova-02", 1L).andExpect(status().isCreated()));
        long count = missionRepository.count();
        long second = createdId(create(token, "richiesta-di-prova-02", 1L).andExpect(status().isCreated()));
        assertEquals(first, second);
        assertEquals(count, missionRepository.count());
    }

    @Test
    void unknownPlateAndWrongPolicyAreIndistinguishable() throws Exception {
        String wrongPolicy = lookup(PLATE, "POLIZZA-ERRATA").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String unknownPlate = lookup("XX000XX", POLICY).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(wrongPolicy, unknownPlate);
        assertEquals(false, JsonPath.read(wrongPolicy, "$.verificato"));
    }

    @Test
    void expiredPolicyIsNotVerified() throws Exception {
        jdbcTemplate.update("UPDATE customers SET scadenza_polizza = DATE '2020-01-01' WHERE id = 2");
        lookup(OTHER_PLATE, OTHER_POLICY).andExpect(jsonPath("$.verificato").value(false));
    }

    @Test
    void repeatedFailuresAreRateLimited() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            lookup("AA111AA", "SBAGLIATA").andExpect(status().isOk());
        }
        lookup("AA111AA", "SBAGLIATA").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("rate_limited"));
    }

    @Test
    void verifiedCustomerCannotActForAnotherCustomer() throws Exception {
        String token = verify(PLATE, POLICY);
        String otherToken = verify(OTHER_PLATE, OTHER_POLICY);
        create(token, "richiesta-di-prova-03", 2L).andExpect(status().isForbidden());

        long missionId = createdId(create(token, "richiesta-di-prova-04", 1L).andExpect(status().isCreated()));
        readStatus(otherToken, missionId).andExpect(status().isNotFound());
        create(otherToken, "richiesta-di-prova-04", 2L).andExpect(status().isConflict());
    }

    @Test
    void fixtureMissionsAreNotReadable() throws Exception {
        Map<String, Object> fixture = jdbcTemplate.queryForMap("""
                SELECT m.id, c.targa_norm, c.numero_polizza FROM missions m
                JOIN customers c ON c.id = m.customer_id
                WHERE m.is_fixture AND c.scadenza_polizza >= CURRENT_DATE
                ORDER BY m.id LIMIT 1
                """);
        String token = verify((String) fixture.get("targa_norm"), (String) fixture.get("numero_polizza"));
        readStatus(token, ((Number) fixture.get("id")).longValue()).andExpect(status().isNotFound());
    }

    @Test
    void missingOrInvalidVerificationTokenIsUnauthorized() throws Exception {
        readStatus("non-valido", 1L).andExpect(status().isUnauthorized());
        mockMvc.perform(asBot(get("/api/v1/chat/missioni/{id}/stato", 1L)))
                .andExpect(status().isUnauthorized());
        create("non-valido", "richiesta-di-prova-05", 1L).andExpect(status().isUnauthorized());
    }

    @Test
    void centreConvertsChatMissionToTowing() throws Exception {
        long missionId = createdId(create(verify(PLATE, POLICY), "richiesta-di-prova-06", 1L));

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

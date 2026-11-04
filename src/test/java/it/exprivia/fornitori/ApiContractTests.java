package it.exprivia.fornitori;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import it.exprivia.fornitori.config.SecurityConfig;
import it.exprivia.fornitori.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(SecurityConfig.class)
class ApiContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean RequestRateLimiter rateLimiter;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean DatabaseUserDetailsService userDetailsService;
    @MockitoBean AuthService auth;
    @MockitoBean RegistryService registry;
    @MockitoBean MissionService missions;
    @MockitoBean ChatService chat;
    @MockitoBean MapService maps;
    @MockitoBean NearestDriverService nearestDriver;
    @MockitoBean CentreAlertService centreAlerts;

    @Test
    void diagnosticsDoNotExposeLoginOrCsrfSecrets() {
        String secret = java.util.UUID.randomUUID().toString();
        org.junit.jupiter.api.Assertions.assertFalse(
                new it.exprivia.fornitori.dto.AuthDtos.LoginRequest("test@example.test", secret).toString().contains(secret));
        org.junit.jupiter.api.Assertions.assertFalse(
                new it.exprivia.fornitori.dto.AuthDtos.CsrfTokenResponse(secret, "X-CSRF-TOKEN").toString().contains(secret));
    }

    @Test @WithMockUser(roles = "RESPONSABILE")
    void oversizedCustomerIs400BeforePersistence() throws Exception {
        mvc.perform(post("/api/v1/clienti").with(csrf()).contentType("application/json")
                .content("""
                    {"nome":"%s","telefono":"123","targa":"AA123BB","compagniaId":1,
                     "numeroPolizza":"P1","scadenzaPolizza":"2030-12-31"}
                    """.formatted("n".repeat(151))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_request"));
        verifyNoInteractions(registry);
    }

    @ParameterizedTest @ValueSource(strings = {"-1", "1.001", "100000000"})
    @WithMockUser(roles = "AUTISTA")
    void invalidDistanceIs400(String km) throws Exception {
        mvc.perform(post("/api/v1/missioni/1/complete").with(csrf()).contentType("application/json")
                .content("{\"kmEffettivi\":" + km + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(missions);
    }

    @Test @WithMockUser(roles = "RESPONSABILE")
    void malformedIdentifierUsesApiError() throws Exception {
        mvc.perform(get("/api/v1/missioni/not-a-number"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test @WithMockUser(roles = "RESPONSABILE")
    void missingCsrfPreventsMutation() throws Exception {
        mvc.perform(post("/api/v1/missioni").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("forbidden"));
        verifyNoInteractions(missions);
    }

    @Test @WithMockUser(roles = "RESPONSABILE")
    void routeWithPersistenceRequiresCsrfEvenOnGet() throws Exception {
        mvc.perform(get("/api/v1/maps/route").param("missionId", "1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(maps);
    }

    @Test @WithMockUser(roles = "RESPONSABILE")
    void implicitHeadRouteCannotBypassCsrf() throws Exception {
        mvc.perform(head("/api/v1/maps/route").param("missionId", "1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(maps);
    }

    @Test @WithMockUser(roles = "AUTISTA")
    void driverCannotCreateOrAdjustCost() throws Exception {
        mvc.perform(post("/api/v1/missioni").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/missioni/1/cost").with(csrf()).contentType("application/json")
                .content("{\"costoFinale\":10,\"motivo\":\"test\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(missions);
    }

    @Test @WithMockUser(roles = "RESPONSABILE")
    void malformedJsonIsSanitized() throws Exception {
        mvc.perform(post("/api/v1/clienti").with(csrf()).contentType("application/json").content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}

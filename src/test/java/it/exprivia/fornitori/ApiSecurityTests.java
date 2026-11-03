package it.exprivia.fornitori;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.exprivia.fornitori.config.SecurityConfig;
import it.exprivia.fornitori.dto.AuthDtos;
import it.exprivia.fornitori.dto.RegistryDtos;
import it.exprivia.fornitori.service.AuthService;
import it.exprivia.fornitori.service.DatabaseUserDetailsService;
import it.exprivia.fornitori.service.MissionService;
import it.exprivia.fornitori.service.RegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

@WebMvcTest
@Import(SecurityConfig.class)
class ApiSecurityTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private DatabaseUserDetailsService userDetailsService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RegistryService registryService;

    @MockitoBean
    private MissionService missionService;

    @Test
    void unauthenticatedRegistryRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/autisti"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    @WithMockUser(username = "driver@example.test", roles = "AUTISTA")
    void driverCannotReadCentralRegistry() throws Exception {
        mockMvc.perform(get("/api/v1/clienti"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @WithMockUser(username = "manager@example.test", roles = "RESPONSABILE")
    void managerCanReadRegistry() throws Exception {
        when(registryService.drivers()).thenReturn(java.util.List.of(
                new RegistryDtos.DriverDto(1L, "Test Driver", "000", 1L, "Test", "disponibile")));

        mockMvc.perform(get("/api/v1/autisti"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponibilita").value("disponibile"));
    }

    @Test
    void invalidJsonLoginReturnsUnauthorizedWithoutLeakingDetails() throws Exception {
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any(Authentication.class));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"manager@example.test\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void successfulJsonLoginReturnsUserWithoutPassword() throws Exception {
        var principal = User.withUsername("manager@example.test")
                .password("not-returned")
                .roles("RESPONSABILE")
                .build();
        var authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authService.currentUser("manager@example.test"))
                .thenReturn(new AuthDtos.CurrentUser(1L, "Manager", "manager@example.test", "responsabile", null));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"manager@example.test\",\"password\":\"submitted-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("manager@example.test"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(session);
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("manager@example.test"));
        mockMvc.perform(post("/api/v1/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    }

    @Test
    void csrfEndpointReturnsTheExpectedHeaderName() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    @Test
    void unauthenticatedMissionRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/missioni"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    @WithMockUser(username = "driver@example.test", roles = "AUTISTA")
    void driverCanAccessOwnMissionEndpointsButNotCentralMissionList() throws Exception {
        when(missionService.driverMissions("driver@example.test")).thenReturn(java.util.List.of());
        when(missionService.list("driver@example.test", null, "live"))
                .thenThrow(new it.exprivia.fornitori.common.ForbiddenOperationException());

        mockMvc.perform(get("/api/v1/autista/missioni"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/missioni"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @WithMockUser(username = "manager@example.test", roles = "RESPONSABILE")
    void managerCanReadLiveMissionList() throws Exception {
        when(missionService.list("manager@example.test", null, "live")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/missioni"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "driver@example.test", roles = "AUTISTA")
    void driverCannotProposeMissionToAnotherDriver() throws Exception {
        mockMvc.perform(post("/api/v1/missioni/1/proposte")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"autistaId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@example.test", roles = "RESPONSABILE")
    void managerCannotAcceptMissionAsDriver() throws Exception {
        mockMvc.perform(post("/api/v1/missioni/1/accept").with(csrf()))
                .andExpect(status().isForbidden());
    }
}

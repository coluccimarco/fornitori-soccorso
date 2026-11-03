package it.exprivia.fornitori;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.exprivia.fornitori.config.SecurityConfig;
import it.exprivia.fornitori.dto.ChatDtos;
import it.exprivia.fornitori.service.AuthService;
import it.exprivia.fornitori.service.ChatService;
import it.exprivia.fornitori.service.DatabaseUserDetailsService;
import it.exprivia.fornitori.service.MissionService;
import it.exprivia.fornitori.service.RegistryService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class ChatApiSecurityTests {
    private static final String LOOKUP_BODY = """
            {"sessionId":"sessione-di-prova-01","targa":"AB123CD"}
            """;

    abstract static class ChatWebTest {
        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        AuthenticationManager authenticationManager;

        @MockitoBean
        DatabaseUserDetailsService userDetailsService;

        @MockitoBean
        AuthService authService;

        @MockitoBean
        RegistryService registryService;

        @MockitoBean
        MissionService missionService;

        @MockitoBean
        ChatService chatService;
    }

    @Nested
    @WebMvcTest
    @Import(SecurityConfig.class)
    @TestPropertySource(properties = "fornitori.chat.service-token=token-di-prova")
    class WithConfiguredToken extends ChatWebTest {
        @Test
        void missingTokenIsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/chat/lookup-targa")
                            .contentType(MediaType.APPLICATION_JSON).content(LOOKUP_BODY))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("unauthorized"));
            verifyNoInteractions(chatService);
        }

        @Test
        void wrongTokenIsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/chat/notifiche").param("canale", "telegram")
                            .header("Authorization", "Bearer token-sbagliato"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(chatService);
        }

        @Test
        void serviceTokenWorksWithoutCsrfAndSession() throws Exception {
            when(chatService.lookup(any())).thenReturn(ChatDtos.VehicleLookupResponse.notVerified());
            mockMvc.perform(post("/api/v1/chat/lookup-targa")
                            .header("Authorization", "Bearer token-di-prova")
                            .contentType(MediaType.APPLICATION_JSON).content(LOOKUP_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verificato").value(false));
        }

        @Test
        void invalidBodyIsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/chat/lookup-targa")
                            .header("Authorization", "Bearer token-di-prova")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"corta\",\"targa\":\"A\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(chatService);
        }

        @Test
        void serviceTokenDoesNotOpenOtherApis() throws Exception {
            mockMvc.perform(get("/api/v1/clienti").header("Authorization", "Bearer token-di-prova"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @WebMvcTest
    @Import(SecurityConfig.class)
    @TestPropertySource(properties = "fornitori.chat.service-token=")
    class WithoutConfiguredToken extends ChatWebTest {
        @Test
        void chatApiIsClosed() throws Exception {
            mockMvc.perform(post("/api/v1/chat/lookup-targa")
                            .header("Authorization", "Bearer ")
                            .contentType(MediaType.APPLICATION_JSON).content(LOOKUP_BODY))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(chatService);
        }
    }
}

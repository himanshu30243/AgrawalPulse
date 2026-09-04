package com.agrawalpulse.user.controller;

import com.agrawalpulse.common.security.SecurityConfig;
import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// First test class UserController has had - scoped to the new self-only /me/chapter endpoint
// (the piece this session's chapter-at-family-registration change actually added).
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, UserControllerTest.JwtDecoderTestConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private CurrentTenantResolver tenantResolver;

    @TestConfiguration
    static class JwtDecoderTestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("not used - tests inject auth via jwt() post-processor");
            };
        }
    }

    @Test
    void updateOwnChapter_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/chapter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chapterId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateOwnChapter_updatesTheCallersOwnAccount_regardlessOfWhatElseIsInTheToken() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID newChapterId = UUID.randomUUID();
        when(tenantResolver.resolve()).thenReturn(new TenantContext(callerId, UUID.randomUUID(), false, false));

        mockMvc.perform(put("/api/v1/users/me/chapter")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chapterId\":\"" + newChapterId + "\"}"))
                .andExpect(status().isOk());

        // The target user always comes from the resolved tenant (the caller's own JWT subject),
        // never from anything in the request body - there is no userId field to send one through.
        verify(userService).updateOwnChapter(eq(callerId), eq(newChapterId));
    }

    @Test
    void updateOwnChapter_missingChapterId_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/chapter")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

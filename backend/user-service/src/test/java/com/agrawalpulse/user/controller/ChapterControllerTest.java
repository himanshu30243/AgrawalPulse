package com.agrawalpulse.user.controller;

import com.agrawalpulse.common.security.SecurityConfig;
import com.agrawalpulse.user.dto.ChapterDto;
import com.agrawalpulse.user.service.ChapterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// First test class ChapterController has had - scoped to this session's two new endpoints
// (resolve, unstaffed) plus the baseline unauthenticated case, following FamilyControllerTest's
// @WebMvcTest conventions.
@WebMvcTest(ChapterController.class)
@Import({SecurityConfig.class, ChapterControllerTest.JwtDecoderTestConfig.class})
class ChapterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChapterService chapterService;

    @TestConfiguration
    static class JwtDecoderTestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("not used - tests inject auth via jwt() post-processor");
            };
        }
    }

    private static ChapterDto chapterDto() {
        return new ChapterDto(UUID.randomUUID(), "Pune Chapter", "Pune", "Maharashtra", Instant.now());
    }

    @Test
    void resolveChapter_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/chapters/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"Pune\",\"state\":\"Maharashtra\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resolveChapter_forbiddenWithoutEditFamilyPermission() throws Exception {
        mockMvc.perform(post("/api/v1/chapters/resolve")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"Pune\",\"state\":\"Maharashtra\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resolveChapter_succeedsForAnyoneHoldingEditFamily() throws Exception {
        when(chapterService.resolveOrCreateChapter("Pune", "Maharashtra")).thenReturn(chapterDto());

        mockMvc.perform(post("/api/v1/chapters/resolve")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_EDIT_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"Pune\",\"state\":\"Maharashtra\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Pune"))
                .andExpect(jsonPath("$.state").value("Maharashtra"));
    }

    @Test
    void listUnstaffedChapters_forbiddenWithoutManageBranches() throws Exception {
        mockMvc.perform(get("/api/v1/chapters/unstaffed")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUnstaffedChapters_succeedsForBranchManager() throws Exception {
        when(chapterService.listUnstaffedChapters()).thenReturn(List.of(chapterDto()));

        mockMvc.perform(get("/api/v1/chapters/unstaffed")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_MANAGE_BRANCHES"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Pune"));
    }

    @Test
    void getChapter_isReachableByAnyAuthenticatedCaller_noSpecificPermissionNeeded() throws Exception {
        UUID chapterId = UUID.randomUUID();
        when(chapterService.getChapter(any())).thenReturn(chapterDto());

        mockMvc.perform(get("/api/v1/chapters/{chapterId}", chapterId).with(jwt()))
                .andExpect(status().isOk());
    }
}

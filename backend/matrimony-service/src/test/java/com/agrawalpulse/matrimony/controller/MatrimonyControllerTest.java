package com.agrawalpulse.matrimony.controller;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.security.SecurityConfig;
import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.matrimony.dto.ConsentDto;
import com.agrawalpulse.matrimony.dto.EligibleSearchCriteria;
import com.agrawalpulse.matrimony.dto.GiveConsentRequest;
import com.agrawalpulse.matrimony.dto.MatrimonyProfileDto;
import com.agrawalpulse.matrimony.entity.ConsentScope;
import com.agrawalpulse.matrimony.service.MatrimonyService;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Controller-slice tests: only the web/security layer is loaded (@WebMvcTest), MatrimonyService
// and CurrentTenantResolver are mocked - the business logic they contain is already covered by
// MatrimonyServiceImplTest. What THIS class verifies is HTTP-specific and security-specific:
// status codes, request/response JSON shape, and - most importantly for this controller -
// that @PreAuthorize enforcement actually matches docs/api-specifications.md: consent capture
// needs no special role, but the eligible-search endpoints require MATRIMONY_VIEWER specifically.
@WebMvcTest(MatrimonyController.class)
@Import({SecurityConfig.class, MatrimonyControllerTest.JwtDecoderTestConfig.class})
class MatrimonyControllerTest {

    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final UUID OTHER_CHAPTER_ID = UUID.randomUUID();
    private static final UUID FAMILY_MEMBER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatrimonyService matrimonyService;

    @MockBean
    private CurrentTenantResolver tenantResolver;

    // A JwtDecoder bean must exist for SecurityConfig's oauth2ResourceServer().jwt(...) DSL to
    // build the filter chain at all, even though every test here injects its Authentication
    // directly via the jwt() request post-processor and never actually calls this decoder.
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
    void giveConsent_requiresAuthenticationButNotMatrimonyViewerRole() throws Exception {
        // The real bug this guards against: an earlier version gated the whole /matrimony
        // surface behind MATRIMONY_VIEWER, which would have blocked a member from ever opting
        // themselves in. A plain authenticated caller, no special roles, must still get 200.
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        ConsentDto savedConsent = new ConsentDto(UUID.randomUUID(), FAMILY_MEMBER_ID, true, ConsentScope.NATIONAL,
                Instant.now(), null);
        when(matrimonyService.giveConsent(eq(CHAPTER_ID), any(GiveConsentRequest.class))).thenReturn(savedConsent);

        GiveConsentRequest request = new GiveConsentRequest(FAMILY_MEMBER_ID, ConsentScope.NATIONAL);

        mockMvc.perform(post("/api/v1/matrimony/consent")
                        .with(jwt().authorities(Collections.emptyList()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familyMemberId").value(FAMILY_MEMBER_ID.toString()))
                .andExpect(jsonPath("$.consentGiven").value(true))
                .andExpect(jsonPath("$.consentScope").value("NATIONAL"));

        verify(matrimonyService).giveConsent(eq(CHAPTER_ID), eq(request));
    }

    @Test
    void giveConsent_withoutAuthentication_returns401() throws Exception {
        GiveConsentRequest request = new GiveConsentRequest(FAMILY_MEMBER_ID, ConsentScope.CHAPTER);

        mockMvc.perform(post("/api/v1/matrimony/consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void giveConsent_missingFamilyMemberId_returns400WithFieldDetail() throws Exception {
        String bodyMissingFamilyMemberId = "{\"consentScope\":\"CHAPTER\"}";

        mockMvc.perform(post("/api/v1/matrimony/consent")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyMissingFamilyMemberId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("familyMemberId")));
    }

    @Test
    void revokeConsent_returns204AndDelegatesWithResolvedChapter() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));

        mockMvc.perform(delete("/api/v1/matrimony/consent/{familyMemberId}", FAMILY_MEMBER_ID)
                        .with(jwt().authorities(Collections.emptyList())))
                .andExpect(status().isNoContent());

        verify(matrimonyService).revokeConsent(CHAPTER_ID, FAMILY_MEMBER_ID);
    }

    @Test
    void listEligible_withoutMatrimonyDirectoryPermission_returns403() throws Exception {
        // Authenticated and otherwise privileged, but without VIEW_MATRIMONY_DIRECTORY - the flip
        // side of the consent test above: directory access is NOT open to every authenticated
        // member, and is deliberately not implied by holding admin-ish permissions either (the
        // DPDP separation that used to be the MATRIMONY_VIEWER role).
        mockMvc.perform(get("/api/v1/matrimony/eligible")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("PERM_VIEW_FAMILY"),
                                new SimpleGrantedAuthority("PERM_MANAGE_USERS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listEligible_withMatrimonyViewerRole_mapsQueryParamsAndReturnsResults() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, true));
        MatrimonyProfileDto profile = new MatrimonyProfileDto(FAMILY_MEMBER_ID, 24, Gender.FEMALE,
                "B.Tech", "Engineer", "Indore", ConsentScope.NATIONAL);
        when(matrimonyService.listEligibleProfiles(eq(CHAPTER_ID), any(EligibleSearchCriteria.class)))
                .thenReturn(List.of(profile));

        mockMvc.perform(get("/api/v1/matrimony/eligible")
                        .param("district", "Indore")
                        .param("gender", "FEMALE")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MATRIMONY_DIRECTORY"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].familyMemberId").value(FAMILY_MEMBER_ID.toString()))
                .andExpect(jsonPath("$[0].district").value("Indore"));

        verify(matrimonyService).listEligibleProfiles(CHAPTER_ID,
                new EligibleSearchCriteria("Indore", null, null, Gender.FEMALE));
    }

    @Test
    void listEligible_crossChapterQueryWithoutNationalRole_returns403() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, true));

        mockMvc.perform(get("/api/v1/matrimony/eligible")
                        .param("chapterId", OTHER_CHAPTER_ID.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MATRIMONY_DIRECTORY"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listEligible_crossChapterQueryWithNationalRole_isAllowed() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, true, true));
        when(matrimonyService.listEligibleProfiles(eq(OTHER_CHAPTER_ID), any(EligibleSearchCriteria.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/matrimony/eligible")
                        .param("chapterId", OTHER_CHAPTER_ID.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MATRIMONY_DIRECTORY"))))
                .andExpect(status().isOk());

        verify(matrimonyService).listEligibleProfiles(eq(OTHER_CHAPTER_ID), any(EligibleSearchCriteria.class));
    }

    @Test
    void getEligible_notFound_returns404WithMessage() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, true));
        when(matrimonyService.getEligibleProfile(CHAPTER_ID, FAMILY_MEMBER_ID))
                .thenThrow(new ResourceNotFoundException("No consented, eligible matrimony profile found for: " + FAMILY_MEMBER_ID));

        mockMvc.perform(get("/api/v1/matrimony/eligible/{familyMemberId}", FAMILY_MEMBER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MATRIMONY_DIRECTORY"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(FAMILY_MEMBER_ID.toString())));
    }
}

package com.agrawalpulse.family.controller;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.security.SecurityConfig;
import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.family.dto.CreateFamilyMemberRequest;
import com.agrawalpulse.family.dto.CreateFamilyRequest;
import com.agrawalpulse.family.dto.FamilyDto;
import com.agrawalpulse.family.dto.FamilyMemberDto;
import com.agrawalpulse.family.dto.UpdateFamilyRequest;
import com.agrawalpulse.family.entity.RelationshipToHead;
import com.agrawalpulse.family.entity.Samaj;
import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.model.MaritalStatus;
import com.agrawalpulse.family.service.FamilyAccessScope;
import com.agrawalpulse.family.service.FamilyService;
import com.agrawalpulse.family.storage.FamilyPhotoData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Controller-slice tests for FamilyController: only the web/security layer is loaded
// (@WebMvcTest), FamilyService and CurrentTenantResolver are mocked - business logic is already
// covered by FamilyServiceImplTest. This class verifies HTTP-specific and security-specific
// behavior: status codes, request validation, and role gating per docs/api-specifications.md.
@WebMvcTest(FamilyController.class)
@Import({SecurityConfig.class, FamilyControllerTest.JwtDecoderTestConfig.class})
class FamilyControllerTest {

    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final UUID OTHER_CHAPTER_ID = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FamilyService familyService;

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
    void createFamily_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalCreateRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFamily_forbiddenForPlainMemberRole() throws Exception {
        mockMvc.perform(post("/api/v1/families")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFamily_succeedsForChapterAdmin() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        FamilyDto created = familyDto("Ramesh Agrawal");
        when(familyService.createFamily(any(), anyBoolean(), any(CreateFamilyRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/families")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"),
                                new SimpleGrantedAuthority("PERM_CREATE_FAMILY"),
                                new SimpleGrantedAuthority("PERM_EDIT_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalCreateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familyCode").value("FAM-ABCD1234"))
                .andExpect(jsonPath("$.headOfFamilyName").value("Ramesh Agrawal"));
    }

    @Test
    void createFamily_succeedsWithBlankOptionalFields() throws Exception {
        // Regression test: the frontend wizard sends "" (not null) for every optional field left
        // blank - aadhaarNumber's @Pattern regex originally required exactly 12 digits with no
        // allowance for an empty string, so every real submission that omitted it 400'd. null
        // bypasses Bean Validation's @Pattern check entirely (only @NotNull validates null), so
        // minimalCreateRequest()'s use of null never exercised this path - this test uses blank
        // strings specifically to guard against that regression class.
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(familyService.createFamily(any(), anyBoolean(), any(CreateFamilyRequest.class))).thenReturn(familyDto("Suresh Sharma"));
        CreateFamilyRequest request = new CreateFamilyRequest("Suresh", "", "Sharma", "", Gender.MALE,
                LocalDate.of(1980, 1, 1), "9998887766", "", "", "1 Test Road", "India", "Madhya Pradesh", "Indore",
                "Test Area", "452001", Samaj.AGRAWAL, "Garg", "Indore", "", "", null, false, false, false, false, false);

        mockMvc.perform(post("/api/v1/families")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"),
                                new SimpleGrantedAuthority("PERM_CREATE_FAMILY"),
                                new SimpleGrantedAuthority("PERM_EDIT_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void createFamily_missingRequiredFields_returns400() throws Exception {
        String bodyMissingRequiredFields = "{}";

        // An empty body fails validation on many required fields at once now (not just one, as
        // when headOfFamilyName was the sole required field) - Bean Validation doesn't guarantee
        // which field's error lands at details[0], so assert headFirstName appears somewhere in
        // the list rather than at a specific index.
        mockMvc.perform(post("/api/v1/families")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"),
                                new SimpleGrantedAuthority("PERM_CREATE_FAMILY"),
                                new SimpleGrantedAuthority("PERM_EDIT_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyMissingRequiredFields))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("headFirstName"))));
    }

    @Test
    void listFamilies_accessibleByMemberRole() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(familyService.listFamilies(any(FamilyAccessScope.class), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/families")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"))))
                .andExpect(status().isOk());
    }

    @Test
    void listFamilies_searchParamsBindAndForwardToService() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(familyService.listFamilies(any(FamilyAccessScope.class), eq("Agrawal"), eq("98765"), eq("Vijay Nagar")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/families")
                        .param("headOfFamilyName", "Agrawal")
                        .param("mobileNumber", "98765")
                        .param("areaLocality", "Vijay Nagar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"))))
                .andExpect(status().isOk());

        verify(familyService).listFamilies(any(FamilyAccessScope.class), eq("Agrawal"), eq("98765"), eq("Vijay Nagar"));
    }

    @Test
    void getFamily_notFound_returns404() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(familyService.getFamily(any(FamilyAccessScope.class), eq(FAMILY_ID)))
                .thenThrow(new ResourceNotFoundException("Family not found: " + FAMILY_ID));

        mockMvc.perform(get("/api/v1/families/{familyId}", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addFamilyMember_forbiddenForTreasurerRole() throws Exception {
        CreateFamilyMemberRequest request = new CreateFamilyMemberRequest(
                "Priya Agrawal", RelationshipToHead.DAUGHTER, LocalDate.now().minusYears(23), Gender.FEMALE,
                MaritalStatus.SINGLE, null, null, null, null, null, null, null);

        // TREASURER can view families (see listFamilies_accessibleByMemberRole's broader role
        // set) but must not be able to add members - only ADMIN/CHAPTER_ADMIN can write.
        mockMvc.perform(post("/api/v1/families/{familyId}/members", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void addFamilyMember_succeedsForChapterAdmin() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        FamilyMemberDto created = new FamilyMemberDto(UUID.randomUUID(), FAMILY_ID, "Priya Agrawal",
                RelationshipToHead.DAUGHTER, LocalDate.of(2002, 11, 5), 23, Gender.FEMALE, MaritalStatus.SINGLE,
                null, null, null, null, null, null, null);
        when(familyService.addFamilyMember(any(FamilyAccessScope.class), eq(FAMILY_ID), any(CreateFamilyMemberRequest.class)))
                .thenReturn(created);

        CreateFamilyMemberRequest request = new CreateFamilyMemberRequest(
                "Priya Agrawal", RelationshipToHead.DAUGHTER, LocalDate.of(2002, 11, 5), Gender.FEMALE,
                MaritalStatus.SINGLE, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/families/{familyId}/members", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"),
                                new SimpleGrantedAuthority("PERM_CREATE_FAMILY"),
                                new SimpleGrantedAuthority("PERM_EDIT_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(23));
    }

    @Test
    void censusCandidates_forbiddenWithoutMatrimonyViewerOrAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/families/census-candidates")
                        .param("chapterId", CHAPTER_ID.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void censusCandidates_crossChapterQueryWithoutNationalRole_returns403() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, true));

        mockMvc.perform(get("/api/v1/families/census-candidates")
                        .param("chapterId", OTHER_CHAPTER_ID.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MATRIMONY_DIRECTORY"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void censusCandidates_ownChapterQuery_returnsOk() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, true));
        when(familyService.listCensusCandidates(CHAPTER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/families/census-candidates")
                        .param("chapterId", CHAPTER_ID.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MATRIMONY_DIRECTORY"))))
                .andExpect(status().isOk());

        verify(familyService).listCensusCandidates(CHAPTER_ID);
    }

    @Test
    void uploadFamilyPhoto_forbiddenForMemberRole() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/families/{familyId}/photo", FAMILY_ID)
                        .file(file)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadFamilyPhoto_succeedsForChapterAdmin() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/families/{familyId}/photo", FAMILY_ID)
                        .file(file)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"),
                                new SimpleGrantedAuthority("PERM_CREATE_FAMILY"),
                                new SimpleGrantedAuthority("PERM_EDIT_FAMILY"))))
                .andExpect(status().isOk());

        verify(familyService).uploadFamilyPhoto(any(FamilyAccessScope.class), eq(FAMILY_ID), eq(new byte[]{1, 2, 3}), eq("image/jpeg"));
    }

    @Test
    void uploadFamilyPhoto_rejectedByServiceValidation_returns400() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        doThrow(new IllegalArgumentException("Unsupported photo type - only JPG/JPEG/PNG are accepted"))
                .when(familyService).uploadFamilyPhoto(any(FamilyAccessScope.class), eq(FAMILY_ID), any(), any());
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/families/{familyId}/photo", FAMILY_ID)
                        .file(file)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"),
                                new SimpleGrantedAuthority("PERM_CREATE_FAMILY"),
                                new SimpleGrantedAuthority("PERM_EDIT_FAMILY"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFamilyPhoto_exceedsMaxUploadSize_returns400NotServerError() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        doThrow(new MaxUploadSizeExceededException(2 * 1024 * 1024))
                .when(familyService).uploadFamilyPhoto(any(FamilyAccessScope.class), eq(FAMILY_ID), any(), any());
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/families/{familyId}/photo", FAMILY_ID)
                        .file(file)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"),
                                new SimpleGrantedAuthority("PERM_CREATE_FAMILY"),
                                new SimpleGrantedAuthority("PERM_EDIT_FAMILY"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFamilyPhoto_returnsBytesWithContentType() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(familyService.getFamilyPhoto(any(FamilyAccessScope.class), eq(FAMILY_ID)))
                .thenReturn(new FamilyPhotoData(new byte[]{1, 2, 3}, "image/jpeg"));

        mockMvc.perform(get("/api/v1/families/{familyId}/photo", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void getFamilyPhoto_notFound_returns404() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(familyService.getFamilyPhoto(any(FamilyAccessScope.class), eq(FAMILY_ID)))
                .thenThrow(new ResourceNotFoundException("No photo uploaded for family: " + FAMILY_ID));

        mockMvc.perform(get("/api/v1/families/{familyId}/photo", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY"))))
                .andExpect(status().isNotFound());
    }

    private UpdateFamilyRequest minimalUpdateRequest() {
        return new UpdateFamilyRequest("Ramesh", null, "Agrawal", "9876543210", null,
                "India", "Madhya Pradesh", "Indore");
    }

    @Test
    void updateFamily_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/families/{familyId}", FAMILY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalUpdateRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateFamily_forbiddenWithoutEditFamilyPermission() throws Exception {
        mockMvc.perform(put("/api/v1/families/{familyId}", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalUpdateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFamily_succeedsForTheOwningMember() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(familyService.updateFamily(any(FamilyAccessScope.class), eq(FAMILY_ID), any(UpdateFamilyRequest.class)))
                .thenReturn(familyDto("Suresh Sharma"));

        mockMvc.perform(put("/api/v1/families/{familyId}", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_EDIT_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headOfFamilyName").value("Suresh Sharma"));
    }

    @Test
    void updateFamily_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/families/{familyId}", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_EDIT_FAMILY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // Every field marked @NotBlank/@NotNull on CreateFamilyRequest must be populated here - an
    // incomplete "minimal" request now 400s on validation before @PreAuthorize's role check ever
    // runs (argument binding/validation happens during Spring MVC's method-argument resolution,
    // which precedes the AOP-proxied security check), which would make the 403-role-check tests
    // that reuse this helper fail with 400 instead for the wrong reason.
    private CreateFamilyRequest minimalCreateRequest() {
        return new CreateFamilyRequest("Ramesh", null, "Agrawal", null, Gender.MALE, LocalDate.of(1975, 3, 14),
                "9876543210", null, null, "12 MG Road", "India", "Madhya Pradesh", "Indore", "Vijay Nagar",
                "452010", Samaj.AGRAWAL, "Garg", "Ujjain", null, null, null, null, null, null, null, null);
    }

    private FamilyDto familyDto(String headOfFamilyName) {
        return new FamilyDto(FAMILY_ID, "FAM-ABCD1234", CHAPTER_ID, null,
                "Ramesh", null, "Agrawal", headOfFamilyName, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, false, false, false, false, null, false, null, null);
    }
}

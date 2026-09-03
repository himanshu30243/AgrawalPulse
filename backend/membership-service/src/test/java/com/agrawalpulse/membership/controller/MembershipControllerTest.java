package com.agrawalpulse.membership.controller;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.security.SecurityConfig;
import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.membership.dto.CollectionSummaryDto;
import com.agrawalpulse.membership.dto.MembershipStatusDto;
import com.agrawalpulse.membership.dto.MembershipTransactionDto;
import com.agrawalpulse.membership.dto.RecordTransactionRequest;
import com.agrawalpulse.membership.dto.UpdateTransactionRequest;
import com.agrawalpulse.membership.entity.MembershipStatus;
import com.agrawalpulse.membership.entity.PaymentMethod;
import com.agrawalpulse.membership.service.MembershipAccessScope;
import com.agrawalpulse.membership.service.MembershipService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Controller-slice tests for MembershipController: only the web/security layer is loaded
// (@WebMvcTest), MembershipService and CurrentTenantResolver are mocked - business logic is
// already covered by MembershipServiceImplTest. This class exists specifically to regression-test
// the two things a service-only test suite can't see: the RBAC gap where write endpoints used to
// accept the view-only permission, and the own-family-vs-another-family 404 isolation contract.
@WebMvcTest(MembershipController.class)
@Import({SecurityConfig.class, MembershipControllerTest.JwtDecoderTestConfig.class})
class MembershipControllerTest {

    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final UUID TRANSACTION_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MembershipService membershipService;

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
    void getStatus_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/memberships/family/{familyId}/status", FAMILY_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStatus_ownFamily_returns200() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(membershipService.getStatus(any(MembershipAccessScope.class), eq(FAMILY_ID)))
                .thenReturn(new MembershipStatusDto(FAMILY_ID, MembershipStatus.ACTIVE, 2026, true,
                        LocalDate.of(2026, 5, 1), 2026));

        mockMvc.perform(get("/api/v1/memberships/family/{familyId}/status", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // The requirement #1 isolation regression test: a family the service layer says is out of the
    // caller's scope must come back 404 through the HTTP layer too, not leak as a 403/500.
    @Test
    void getStatus_familyOutOfCallersScope_returns404() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(membershipService.getStatus(any(MembershipAccessScope.class), eq(FAMILY_ID)))
                .thenThrow(new ResourceNotFoundException("Family not found: " + FAMILY_ID));

        mockMvc.perform(get("/api/v1/memberships/family/{familyId}/status", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactionHistory_ownFamily_returns200() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(membershipService.getTransactionHistory(any(MembershipAccessScope.class), eq(FAMILY_ID)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/memberships/family/{familyId}/transactions", FAMILY_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP"))))
                .andExpect(status().isOk());
    }

    // The actual security-bug regression test: recording a transaction must require
    // PERM_MANAGE_MEMBERSHIP - a caller holding only the view permission must be forbidden, where
    // the old MembershipController accepted PERM_VIEW_MEMBERSHIP for this endpoint.
    @Test
    void recordTransaction_forbiddenWithViewOnlyPermission() throws Exception {
        mockMvc.perform(post("/api/v1/memberships/transactions")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRecordRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void recordTransaction_succeedsWithManagePermission() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(membershipService.recordTransaction(any(MembershipAccessScope.class), any(RecordTransactionRequest.class)))
                .thenReturn(new MembershipTransactionDto(TRANSACTION_ID, FAMILY_ID, 2026, BigDecimal.valueOf(250),
                        LocalDate.now(), PaymentMethod.CASH, "TXN-1", null, UUID.randomUUID(), null, null, null));

        mockMvc.perform(post("/api/v1/memberships/transactions")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP"),
                                new SimpleGrantedAuthority("PERM_MANAGE_MEMBERSHIP")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRecordRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID.toString()));
    }

    @Test
    void recordTransaction_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/memberships/transactions")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP"),
                                new SimpleGrantedAuthority("PERM_MANAGE_MEMBERSHIP")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTransaction_forbiddenWithViewOnlyPermission() throws Exception {
        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.valueOf(250), LocalDate.now(),
                PaymentMethod.CASH, "TXN-1", null);

        mockMvc.perform(put("/api/v1/memberships/transactions/{transactionId}", TRANSACTION_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTransaction_succeedsWithManagePermission() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.valueOf(300), LocalDate.now(),
                PaymentMethod.UPI, "TXN-2", "Corrected");
        when(membershipService.updateTransaction(any(MembershipAccessScope.class), eq(TRANSACTION_ID),
                any(UpdateTransactionRequest.class)))
                .thenReturn(new MembershipTransactionDto(TRANSACTION_ID, FAMILY_ID, 2026, BigDecimal.valueOf(300),
                        LocalDate.now(), PaymentMethod.UPI, "TXN-2", "Corrected", UUID.randomUUID(), null,
                        UUID.randomUUID(), null));

        mockMvc.perform(put("/api/v1/memberships/transactions/{transactionId}", TRANSACTION_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP"),
                                new SimpleGrantedAuthority("PERM_MANAGE_MEMBERSHIP")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(300));
    }

    @Test
    void listMembers_bindsQueryParamsAndReturns200() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(membershipService.listMembers(any(MembershipAccessScope.class), eq(2026), eq(MembershipStatus.EXPIRED)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/memberships/members")
                        .param("financialYear", "2026")
                        .param("status", "EXPIRED")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP"))))
                .andExpect(status().isOk());

        verify(membershipService).listMembers(any(MembershipAccessScope.class), eq(2026), eq(MembershipStatus.EXPIRED));
    }

    @Test
    void pendingPaymentReport_bindsAllSearchFilters() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(membershipService.pendingPaymentReport(any(MembershipAccessScope.class), eq(2026), eq("FAM-1"),
                eq("Agrawal"), eq("98765"), eq("Vijay Nagar"))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/memberships/reports/pending")
                        .param("financialYear", "2026")
                        .param("familyId", "FAM-1")
                        .param("headOfFamilyName", "Agrawal")
                        .param("mobileNumber", "98765")
                        .param("areaLocality", "Vijay Nagar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP"))))
                .andExpect(status().isOk());

        verify(membershipService).pendingPaymentReport(any(MembershipAccessScope.class), eq(2026), eq("FAM-1"),
                eq("Agrawal"), eq("98765"), eq("Vijay Nagar"));
    }

    @Test
    void collectionSummary_returns200() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(membershipService.collectionSummary(any(MembershipAccessScope.class), eq(2026)))
                .thenReturn(new CollectionSummaryDto(CHAPTER_ID, "Indore Chapter", 2026, BigDecimal.valueOf(2500),
                        10, 3, 2));

        mockMvc.perform(get("/api/v1/memberships/reports/collection-summary")
                        .param("financialYear", "2026")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_MEMBERSHIP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familiesActive").value(10));
    }

    private RecordTransactionRequest validRecordRequest() {
        return new RecordTransactionRequest(FAMILY_ID, 2026, BigDecimal.valueOf(250), LocalDate.now(),
                PaymentMethod.CASH, "TXN-1", null);
    }
}

package com.agrawalpulse.membership.controller;

import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.membership.dto.CollectionSummaryDto;
import com.agrawalpulse.membership.dto.MembershipReportRow;
import com.agrawalpulse.membership.dto.MembershipStatusDto;
import com.agrawalpulse.membership.dto.MembershipTransactionDto;
import com.agrawalpulse.membership.dto.RecordTransactionRequest;
import com.agrawalpulse.membership.dto.UpdateTransactionRequest;
import com.agrawalpulse.membership.entity.MembershipStatus;
import com.agrawalpulse.membership.service.MembershipAccessScope;
import com.agrawalpulse.membership.service.MembershipService;
import com.agrawalpulse.membership.util.FinancialYearUtil;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memberships")
public class MembershipController {

    private final MembershipService membershipService;
    private final CurrentTenantResolver tenantResolver;

    public MembershipController(MembershipService membershipService, CurrentTenantResolver tenantResolver) {
        this.membershipService = membershipService;
        this.tenantResolver = tenantResolver;
    }

    // Requirement #1 - a member's own status. VIEW_MEMBERSHIP alone (no tier permission) still
    // resolves to "my family only" - see MembershipServiceImpl.getStatus, which delegates the actual
    // authorization to family-service regardless of tier.
    @GetMapping("/family/{familyId}/status")
    @PreAuthorize("hasAuthority('PERM_VIEW_MEMBERSHIP')")
    public MembershipStatusDto getStatus(@PathVariable UUID familyId) {
        return membershipService.getStatus(resolveScope(), familyId);
    }

    // Requirement #1 - a member's own transaction history only, never another family's.
    @GetMapping("/family/{familyId}/transactions")
    @PreAuthorize("hasAuthority('PERM_VIEW_MEMBERSHIP')")
    public List<MembershipTransactionDto> getTransactionHistory(@PathVariable UUID familyId) {
        return membershipService.getTransactionHistory(resolveScope(), familyId);
    }

    // The actual fix for the previously-live security bug: writes require PERM_MANAGE_MEMBERSHIP,
    // not the view-only permission every authenticated user holds.
    @PostMapping("/transactions")
    @PreAuthorize("hasAuthority('PERM_MANAGE_MEMBERSHIP')")
    public MembershipTransactionDto recordTransaction(@Valid @RequestBody RecordTransactionRequest request) {
        return membershipService.recordTransaction(resolveScope(), request);
    }

    @PutMapping("/transactions/{transactionId}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_MEMBERSHIP')")
    public MembershipTransactionDto updateTransaction(@PathVariable UUID transactionId,
                                                        @Valid @RequestBody UpdateTransactionRequest request) {
        return membershipService.updateTransaction(resolveScope(), transactionId, request);
    }

    // Admin "Active/Pending/Expired members" listing. financialYear/status are both optional -
    // financialYear defaults to today's FY, status left unfiltered (all three) when omitted.
    @GetMapping("/members")
    @PreAuthorize("hasAuthority('PERM_VIEW_MEMBERSHIP')")
    public List<MembershipStatusDto> listMembers(
            @RequestParam(required = false) Integer financialYear,
            @RequestParam(required = false) MembershipStatus status) {
        return membershipService.listMembers(resolveScope(), effectiveFinancialYear(financialYear), status);
    }

    @GetMapping("/reports/pending")
    @PreAuthorize("hasAuthority('PERM_VIEW_MEMBERSHIP')")
    public List<MembershipReportRow> pendingPaymentReport(
            @RequestParam(required = false) Integer financialYear,
            @RequestParam(required = false) String familyId,
            @RequestParam(required = false) String headOfFamilyName,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String areaLocality) {
        return membershipService.pendingPaymentReport(resolveScope(), effectiveFinancialYear(financialYear),
                familyId, headOfFamilyName, mobileNumber, areaLocality);
    }

    @GetMapping("/reports/collection-summary")
    @PreAuthorize("hasAuthority('PERM_VIEW_MEMBERSHIP')")
    public CollectionSummaryDto collectionSummary(@RequestParam(required = false) Integer financialYear) {
        return membershipService.collectionSummary(resolveScope(), effectiveFinancialYear(financialYear));
    }

    private int effectiveFinancialYear(Integer requested) {
        return requested != null ? requested : FinancialYearUtil.currentFinancialYear();
    }

    // Built fresh per request from the caller's own JWT-derived tenant/permissions - never cached
    // or reused across requests. Precedence (broadest wins) lives in MembershipAccessScope's javadoc.
    private MembershipAccessScope resolveScope() {
        TenantContext tenant = tenantResolver.resolve();
        return new MembershipAccessScope(
                tenant.requireChapterId(),
                tenant.userId(),
                tenant.hasPermission("VIEW_ALL_MEMBERSHIP"),
                tenant.hasPermission("VIEW_STATE_MEMBERSHIP"),
                tenant.hasPermission("VIEW_CHAPTER_MEMBERSHIP"));
    }
}

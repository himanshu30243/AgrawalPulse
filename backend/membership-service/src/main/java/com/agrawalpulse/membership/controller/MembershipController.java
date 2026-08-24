package com.agrawalpulse.membership.controller;

import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.membership.dto.CreateMembershipRequest;
import com.agrawalpulse.membership.dto.MembershipDto;
import com.agrawalpulse.membership.dto.MembershipPaymentDto;
import com.agrawalpulse.membership.dto.RecordPaymentRequest;
import com.agrawalpulse.membership.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
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

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_VIEW_MEMBERSHIP')")
    public MembershipDto createMembership(@Valid @RequestBody CreateMembershipRequest request) {
        TenantContext tenant = tenantResolver.resolve();
        return membershipService.createMembership(tenant.requireChapterId(), request);
    }

    @GetMapping("/{membershipId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_MEMBERSHIP')")
    public MembershipDto getMembership(@PathVariable UUID membershipId) {
        TenantContext tenant = tenantResolver.resolve();
        return membershipService.getMembership(tenant.requireChapterId(), membershipId);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_VIEW_MEMBERSHIP')")
    public List<MembershipDto> listMemberships(
            @RequestParam(required = false) Integer year) {
        TenantContext tenant = tenantResolver.resolve();
        int effectiveYear = year != null ? year : Year.now().getValue();
        return membershipService.listMembershipsForChapter(tenant.requireChapterId(), effectiveYear);
    }

    @PostMapping("/{membershipId}/payments")
    @PreAuthorize("hasAuthority('PERM_VIEW_MEMBERSHIP')")
    public MembershipPaymentDto recordPayment(@PathVariable UUID membershipId,
                                               @Valid @RequestBody RecordPaymentRequest request) {
        TenantContext tenant = tenantResolver.resolve();
        return membershipService.recordPayment(tenant.requireChapterId(), membershipId, request);
    }

    @GetMapping("/{membershipId}/payments")
    @PreAuthorize("hasAuthority('PERM_VIEW_MEMBERSHIP')")
    public List<MembershipPaymentDto> listPayments(@PathVariable UUID membershipId) {
        TenantContext tenant = tenantResolver.resolve();
        return membershipService.listPayments(tenant.requireChapterId(), membershipId);
    }
}

package com.agrawalpulse.matrimony.controller;

import com.agrawalpulse.common.exception.TenantAccessDeniedException;
import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.matrimony.dto.ConsentDto;
import com.agrawalpulse.matrimony.dto.EligibleSearchCriteria;
import com.agrawalpulse.matrimony.dto.GiveConsentRequest;
import com.agrawalpulse.matrimony.dto.MatrimonyProfileDto;
import com.agrawalpulse.matrimony.service.MatrimonyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matrimony")
public class MatrimonyController {

    private final MatrimonyService matrimonyService;
    private final CurrentTenantResolver tenantResolver;

    public MatrimonyController(MatrimonyService matrimonyService, CurrentTenantResolver tenantResolver) {
        this.matrimonyService = matrimonyService;
        this.tenantResolver = tenantResolver;
    }

    // Deliberately no @PreAuthorize/MATRIMONY_VIEWER here: per docs/api-specifications.md, any
    // authenticated member manages their own (or their dependent's) consent without needing
    // viewer rights - the class-level SecurityConfig "anyRequest().authenticated()" is the only
    // gate. Do not add hasRole('MATRIMONY_VIEWER') to this method; that was a real bug fixed on
    // the frontend for the same reason and must not be reintroduced here.
    @PostMapping("/consent")
    public ConsentDto giveConsent(@Valid @RequestBody GiveConsentRequest request) {
        TenantContext tenant = tenantResolver.resolve();
        return matrimonyService.giveConsent(tenant.requireChapterId(), request);
    }

    @DeleteMapping("/consent/{familyMemberId}")
    public ResponseEntity<Void> revokeConsent(@PathVariable UUID familyMemberId) {
        TenantContext tenant = tenantResolver.resolve();
        matrimonyService.revokeConsent(tenant.requireChapterId(), familyMemberId);
        return ResponseEntity.noContent().build();
    }

    // hasRole('MATRIMONY_VIEWER') specifically - deliberately NOT satisfied by
    // ROLE_ADMIN/ROLE_CHAPTER_ADMIN/ROLE_TREASURER alone, per the DPDP requirement that
    // matrimonial data access is a distinct permission tier from general chapter-admin access
    // (docs/security-design.md). An admin who also needs this must be explicitly granted
    // MATRIMONY_VIEWER.
    @GetMapping("/eligible")
    @PreAuthorize("hasAuthority('PERM_VIEW_MATRIMONY_DIRECTORY')")
    public List<MatrimonyProfileDto> listEligible(@RequestParam(required = false) String district,
                                                   @RequestParam(required = false) String education,
                                                   @RequestParam(required = false) String profession,
                                                   @RequestParam(required = false) Gender gender,
                                                   @RequestParam(required = false) UUID chapterId) {
        TenantContext tenant = tenantResolver.resolve();
        UUID targetChapterId = resolveTargetChapter(tenant, chapterId);
        return matrimonyService.listEligibleProfiles(targetChapterId,
                new EligibleSearchCriteria(district, education, profession, gender));
    }

    @GetMapping("/eligible/{familyMemberId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_MATRIMONY_DIRECTORY')")
    public MatrimonyProfileDto getEligible(@PathVariable UUID familyMemberId,
                                            @RequestParam(required = false) UUID chapterId) {
        TenantContext tenant = tenantResolver.resolve();
        UUID targetChapterId = resolveTargetChapter(tenant, chapterId);
        return matrimonyService.getEligibleProfile(targetChapterId, familyMemberId);
    }

    // chapterId query param is only honored for NATIONAL_ADMIN callers (per
    // docs/api-specifications.md: "chapterId? (national role only)") - everyone else is always
    // scoped to their own JWT-derived chapter, never a client-supplied one.
    private UUID resolveTargetChapter(TenantContext tenant, UUID requestedChapterId) {
        if (requestedChapterId == null) {
            return tenant.requireChapterId();
        }
        if (!tenant.nationalRole()) {
            throw new TenantAccessDeniedException("Only a national role may query another chapter's matrimony data");
        }
        return requestedChapterId;
    }
}

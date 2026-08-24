package com.agrawalpulse.analytics.controller;

import com.agrawalpulse.analytics.dto.ActiveMembershipCountDto;
import com.agrawalpulse.analytics.dto.AnalyticsSummaryDto;
import com.agrawalpulse.analytics.dto.FamilyCountDto;
import com.agrawalpulse.analytics.dto.MarriageReadinessCountDto;
import com.agrawalpulse.analytics.dto.PopulationBreakdownDto;
import com.agrawalpulse.analytics.dto.PopulationCountDto;
import com.agrawalpulse.analytics.service.AnalyticsService;
import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAuthority('PERM_VIEW_REPORTS')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentTenantResolver tenantResolver;

    public AnalyticsController(AnalyticsService analyticsService, CurrentTenantResolver tenantResolver) {
        this.analyticsService = analyticsService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/families/total")
    public FamilyCountDto getTotalFamilies(@RequestParam(required = false) UUID chapterId) {
        return analyticsService.getTotalFamilies(resolveScope(chapterId));
    }

    @GetMapping("/memberships/active")
    public ActiveMembershipCountDto getActiveMemberships(@RequestParam(required = false) UUID chapterId,
                                                           @RequestParam(required = false) Integer year) {
        int effectiveYear = year != null ? year : Year.now().getValue();
        return analyticsService.getActiveMemberships(resolveScope(chapterId), effectiveYear);
    }

    @GetMapping("/marriage-readiness/eligible-count")
    public MarriageReadinessCountDto getMarriageReadinessCounts(@RequestParam(required = false) UUID chapterId) {
        return analyticsService.getMarriageReadinessCounts(resolveScope(chapterId));
    }

    // "city" filters/groups on the chapter's own city (chapters.city), not family.district - see
    // AnalyticsQueryRepository. A NATIONAL_ADMIN calling with no chapterId and no city gets the
    // whole-federation total; that's the "nation" view - there's no separate endpoint for it,
    // omitting both filters on this same endpoint is what "nation" means here.
    @GetMapping("/population/total")
    public PopulationCountDto getTotalPopulation(@RequestParam(required = false) UUID chapterId,
                                                   @RequestParam(required = false) Gender gender,
                                                   @RequestParam(required = false) String city) {
        return analyticsService.getTotalPopulation(resolveScope(chapterId), gender != null ? gender.name() : null, city);
    }

    @GetMapping("/population/by-city")
    public List<PopulationBreakdownDto> getPopulationByCity(@RequestParam(required = false) UUID chapterId,
                                                              @RequestParam(required = false) Gender gender) {
        return analyticsService.getPopulationByCity(resolveScope(chapterId), gender != null ? gender.name() : null);
    }

    @GetMapping("/population/by-state")
    public List<PopulationBreakdownDto> getPopulationByState(@RequestParam(required = false) UUID chapterId,
                                                               @RequestParam(required = false) Gender gender) {
        return analyticsService.getPopulationByState(resolveScope(chapterId), gender != null ? gender.name() : null);
    }

    @GetMapping("/summary")
    public AnalyticsSummaryDto getSummary(@RequestParam(required = false) UUID chapterId) {
        return analyticsService.getSummary(resolveScope(chapterId));
    }

    // A client-supplied chapterId is honored only for callers holding the national role - every
    // other caller is pinned to their own JWT chapter_id, so a chapter admin can never read
    // another chapter's (or the whole federation's) aggregate numbers by tampering with a query param.
    private UUID resolveScope(UUID requestedChapterId) {
        TenantContext tenant = tenantResolver.resolve();
        if (tenant.nationalRole()) {
            return requestedChapterId;
        }
        return tenant.requireChapterId();
    }
}

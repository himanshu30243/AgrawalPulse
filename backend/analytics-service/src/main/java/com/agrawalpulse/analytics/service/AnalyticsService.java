package com.agrawalpulse.analytics.service;

import com.agrawalpulse.analytics.dto.ActiveMembershipCountDto;
import com.agrawalpulse.analytics.dto.AnalyticsSummaryDto;
import com.agrawalpulse.analytics.dto.FamilyCountDto;
import com.agrawalpulse.analytics.dto.MarriageReadinessCountDto;
import com.agrawalpulse.analytics.dto.PopulationBreakdownDto;
import com.agrawalpulse.analytics.dto.PopulationCountDto;

import java.util.List;
import java.util.UUID;

public interface AnalyticsService {

    // chapterId == null means "aggregate across all chapters" - callers must only pass null
    // when the caller holds a national role; the controller enforces that, never this layer's
    // callers, so the same query logic serves both scoped and national requests.
    FamilyCountDto getTotalFamilies(UUID chapterId);

    ActiveMembershipCountDto getActiveMemberships(UUID chapterId, int year);

    MarriageReadinessCountDto getMarriageReadinessCounts(UUID chapterId);

    // gender/city null means unfiltered. chapterId == null (national scope) + no city filter is
    // how a NATIONAL_ADMIN gets the whole-federation population count in one call.
    PopulationCountDto getTotalPopulation(UUID chapterId, String gender, String city);

    List<PopulationBreakdownDto> getPopulationByCity(UUID chapterId, String gender);

    List<PopulationBreakdownDto> getPopulationByState(UUID chapterId, String gender);

    // Comprehensive dashboard summary: aggregates families, population, memberships, and
    // marriage readiness data with chart breakdowns.
    AnalyticsSummaryDto getSummary(UUID chapterId);
}

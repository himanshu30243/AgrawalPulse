package com.agrawalpulse.analytics.dto;

import java.util.List;

public record AnalyticsSummaryDto(
        long totalFamilies,
        long totalPopulation,
        long activeMembers,
        long membershipCollection,
        long eligibleBoys,
        long eligibleGirls,
        List<FamiliesByChapterDto> familiesByChapter,
        List<MembershipStatusCountDto> membershipStatusSplit,
        List<AgeEligibilityDistributionDto> ageEligibilityDistribution
) {
}

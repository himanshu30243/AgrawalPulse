package com.agrawalpulse.analytics.service;

import com.agrawalpulse.analytics.dto.ActiveMembershipCountDto;
import com.agrawalpulse.analytics.dto.AgeEligibilityDistributionDto;
import com.agrawalpulse.analytics.dto.AnalyticsSummaryDto;
import com.agrawalpulse.analytics.dto.FamiliesByChapterDto;
import com.agrawalpulse.analytics.dto.FamilyCountDto;
import com.agrawalpulse.analytics.dto.MarriageReadinessCountDto;
import com.agrawalpulse.analytics.dto.MembershipStatusCountDto;
import com.agrawalpulse.analytics.dto.PopulationBreakdownDto;
import com.agrawalpulse.analytics.dto.PopulationCountDto;
import com.agrawalpulse.analytics.repository.AnalyticsQueryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsQueryRepository queryRepository;
    private final int girlsMinAge;
    private final int boysMinAge;

    public AnalyticsServiceImpl(AnalyticsQueryRepository queryRepository,
                                 @Value("${agrawalpulse.matrimony.readiness.girls-min-age:21}") int girlsMinAge,
                                 @Value("${agrawalpulse.matrimony.readiness.boys-min-age:24}") int boysMinAge) {
        this.queryRepository = queryRepository;
        this.girlsMinAge = girlsMinAge;
        this.boysMinAge = boysMinAge;
    }

    @Override
    // Cached in Redis (ElastiCache in AWS) - dashboard aggregate counts are read far more often
    // than families/memberships change, and this is exactly the "distributed caching" pattern
    // called out in the architecture doc.
    @Cacheable(value = "analytics:familyTotals", key = "#chapterId != null ? #chapterId.toString() : 'ALL'")
    public FamilyCountDto getTotalFamilies(UUID chapterId) {
        return new FamilyCountDto(chapterId, queryRepository.countTotalFamilies(chapterId));
    }

    @Override
    @Cacheable(value = "analytics:activeMemberships",
            key = "(#chapterId != null ? #chapterId.toString() : 'ALL') + ':' + #year")
    public ActiveMembershipCountDto getActiveMemberships(UUID chapterId, int year) {
        return new ActiveMembershipCountDto(chapterId, year, queryRepository.countActiveMemberships(chapterId, year));
    }

    @Override
    @Cacheable(value = "analytics:marriageReadiness", key = "#chapterId != null ? #chapterId.toString() : 'ALL'")
    public MarriageReadinessCountDto getMarriageReadinessCounts(UUID chapterId) {
        LocalDate femaleThreshold = LocalDate.now().minusYears(girlsMinAge);
        LocalDate maleThreshold = LocalDate.now().minusYears(boysMinAge);
        Map<String, Long> byGender = queryRepository.countReadyByGender(chapterId, femaleThreshold, maleThreshold);
        return new MarriageReadinessCountDto(
                chapterId,
                byGender.getOrDefault("FEMALE", 0L),
                byGender.getOrDefault("MALE", 0L),
                byGender.getOrDefault("OTHER", 0L));
    }

    @Override
    @Cacheable(value = "analytics:population", key = "(#chapterId != null ? #chapterId.toString() : 'ALL') "
            + "+ ':' + (#gender != null ? #gender : '-') + ':' + (#city != null ? #city : '-')")
    public PopulationCountDto getTotalPopulation(UUID chapterId, String gender, String city) {
        return new PopulationCountDto(chapterId, gender, city,
                queryRepository.countTotalPopulation(chapterId, gender, city));
    }

    @Override
    @Cacheable(value = "analytics:populationByCity",
            key = "(#chapterId != null ? #chapterId.toString() : 'ALL') + ':' + (#gender != null ? #gender : '-')")
    public List<PopulationBreakdownDto> getPopulationByCity(UUID chapterId, String gender) {
        return queryRepository.countPopulationByCity(chapterId, gender).entrySet().stream()
                .map(e -> new PopulationBreakdownDto(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    @Cacheable(value = "analytics:populationByState",
            key = "(#chapterId != null ? #chapterId.toString() : 'ALL') + ':' + (#gender != null ? #gender : '-')")
    public List<PopulationBreakdownDto> getPopulationByState(UUID chapterId, String gender) {
        return queryRepository.countPopulationByState(chapterId, gender).entrySet().stream()
                .map(e -> new PopulationBreakdownDto(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    @Cacheable(value = "analytics:summary", key = "#chapterId != null ? #chapterId.toString() : 'ALL'")
    public AnalyticsSummaryDto getSummary(UUID chapterId) {
        int currentYear = LocalDate.now().getYear();

        // Aggregate counts
        long totalFamilies = queryRepository.countTotalFamilies(chapterId);
        long totalPopulation = queryRepository.countTotalPopulation(chapterId, null, null);
        long activeMembers = queryRepository.countActiveMemberships(chapterId, currentYear);
        long membershipCollection = queryRepository.getTotalMembershipCollection(chapterId);

        // Marriage readiness counts
        LocalDate femaleThreshold = LocalDate.now().minusYears(girlsMinAge);
        LocalDate maleThreshold = LocalDate.now().minusYears(boysMinAge);
        Map<String, Long> readyByGender = queryRepository.countReadyByGender(chapterId, femaleThreshold, maleThreshold);
        long eligibleBoys = readyByGender.getOrDefault("MALE", 0L);
        long eligibleGirls = readyByGender.getOrDefault("FEMALE", 0L);

        // Families by chapter breakdown
        Map<String, Long> familiesByChapterMap = queryRepository.countFamiliesByChapter(chapterId);
        List<FamiliesByChapterDto> familiesByChapter = familiesByChapterMap.entrySet().stream()
                .map(e -> new FamiliesByChapterDto(e.getKey(), e.getValue()))
                .toList();

        // Membership status split
        Map<String, Long> statusMap = queryRepository.countMembershipsByStatus(chapterId, currentYear);
        List<MembershipStatusCountDto> membershipStatusSplit = statusMap.entrySet().stream()
                .map(e -> new MembershipStatusCountDto(e.getKey(), e.getValue()))
                .toList();

        // Age eligibility distribution by age band
        Map<String, Map<String, Long>> ageDistribution = queryRepository.countAgeEligibilityDistribution(chapterId, femaleThreshold, maleThreshold);
        List<AgeEligibilityDistributionDto> ageEligibilityDistribution = new ArrayList<>();
        String[] ageBands = {"18-19", "20-24", "25-29", "30-34", "35-39", "40+"};
        for (String ageBand : ageBands) {
            Map<String, Long> genderCounts = ageDistribution.getOrDefault(ageBand, Map.of());
            long boys = genderCounts.getOrDefault("MALE", 0L);
            long girls = genderCounts.getOrDefault("FEMALE", 0L);
            ageEligibilityDistribution.add(new AgeEligibilityDistributionDto(ageBand, boys, girls));
        }

        return new AnalyticsSummaryDto(
                totalFamilies,
                totalPopulation,
                activeMembers,
                membershipCollection,
                eligibleBoys,
                eligibleGirls,
                familiesByChapter,
                membershipStatusSplit,
                ageEligibilityDistribution
        );
    }
}

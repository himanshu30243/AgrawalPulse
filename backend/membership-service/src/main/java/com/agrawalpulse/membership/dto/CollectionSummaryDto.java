package com.agrawalpulse.membership.dto;

import java.math.BigDecimal;
import java.util.UUID;

// One chapter's membership collection summary for a given financial year - the admin "Membership
// Collection Summary" report. See MembershipServiceImpl.collectionSummary.
public record CollectionSummaryDto(
        UUID chapterId,
        String chapterName,
        int financialYear,
        BigDecimal totalCollected,
        int familiesActive,
        int familiesPending,
        int familiesExpired
) {
}

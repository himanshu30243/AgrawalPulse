package com.agrawalpulse.membership.dto;

import com.agrawalpulse.membership.entity.MembershipStatus;

import java.time.LocalDate;
import java.util.UUID;

// One row of the admin pending-payment report - backend-composed from family-service's search
// (headOfFamilyName/mobileNumber/areaLocality/chapterName) joined with this service's own
// membership/payment data (status/lastPaidFinancialYear/lastPaymentDate). See
// MembershipServiceImpl.pendingPaymentReport.
public record MembershipReportRow(
        UUID familyId,
        String familyCode,
        String headOfFamilyName,
        String mobileNumber,
        String areaLocality,
        UUID chapterId,
        String chapterName,
        MembershipStatus status,
        Integer lastPaidFinancialYear,
        LocalDate lastPaymentDate
) {
}

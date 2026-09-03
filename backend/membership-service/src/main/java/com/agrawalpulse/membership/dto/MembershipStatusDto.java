package com.agrawalpulse.membership.dto;

import com.agrawalpulse.membership.entity.MembershipStatus;

import java.time.LocalDate;
import java.util.UUID;

// Family-level computed status - the response for "what is my/this family's membership status
// right now", per requirement #1. Never a direct copy of one Membership row: status here can be
// EXPIRED, which no single row ever stores (see MembershipStatus). Computed by
// MembershipServiceImpl.computeStatus from every FY row a family has.
public record MembershipStatusDto(
        UUID familyId,
        MembershipStatus status,
        int currentFinancialYear,
        boolean currentFinancialYearPaid,
        LocalDate lastPaymentDate,
        Integer lastPaidFinancialYear
) {
}

package com.agrawalpulse.membership.dto;

import com.agrawalpulse.membership.entity.MembershipStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MembershipDto(
        UUID id,
        UUID familyId,
        int year,
        BigDecimal feeAmount,
        MembershipStatus status,
        Instant paidAt
) {
}

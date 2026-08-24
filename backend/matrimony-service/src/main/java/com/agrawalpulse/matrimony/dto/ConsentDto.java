package com.agrawalpulse.matrimony.dto;

import com.agrawalpulse.matrimony.entity.ConsentScope;

import java.time.Instant;
import java.util.UUID;

public record ConsentDto(
        UUID id,
        UUID familyMemberId,
        boolean consentGiven,
        ConsentScope consentScope,
        Instant consentedAt,
        Instant revokedAt
) {
}

package com.agrawalpulse.membership.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateMembershipRequest(
        @NotNull UUID familyId,
        @Min(2000) int year
) {
}

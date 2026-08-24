package com.agrawalpulse.matrimony.dto;

import com.agrawalpulse.matrimony.entity.ConsentScope;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GiveConsentRequest(
        @NotNull UUID familyMemberId,
        @NotNull ConsentScope consentScope
) {
}

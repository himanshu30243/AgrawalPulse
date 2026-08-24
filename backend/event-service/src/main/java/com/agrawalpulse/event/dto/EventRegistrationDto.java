package com.agrawalpulse.event.dto;

import java.time.Instant;
import java.util.UUID;

public record EventRegistrationDto(
        UUID id,
        UUID eventId,
        UUID familyId,
        Instant registeredAt
) {
}

package com.agrawalpulse.event.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterFamilyRequest(@NotNull UUID familyId) {
}

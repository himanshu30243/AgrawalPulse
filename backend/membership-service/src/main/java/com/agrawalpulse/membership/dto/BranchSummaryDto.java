package com.agrawalpulse.membership.dto;

import java.util.UUID;

// Trimmed local copy of user-service's chapter summary shape - same "own local copy per service"
// convention as FamilyDto (see that class's comment). Used only for state-tier scope resolution
// (which chapters share a caller's own state) and enriching report rows with a chapter name.
public record BranchSummaryDto(UUID id, String name, String city, String state) {
}

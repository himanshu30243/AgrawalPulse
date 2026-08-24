package com.agrawalpulse.family.dto;

import java.util.UUID;

// "Branch" is this service's display-layer name for what user-service still calls a Chapter
// internally (entity, table, and /api/v1/chapters path all unchanged there - renaming those too
// would be a much larger, riskier change across a service boundary for a purely cosmetic label).
// Attached to FamilyDto so a raw chapterId is never the only thing shown for "which branch".
public record BranchSummaryDto(UUID id, String name, String city, String state) {
}

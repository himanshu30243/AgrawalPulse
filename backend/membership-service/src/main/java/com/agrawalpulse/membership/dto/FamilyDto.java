package com.agrawalpulse.membership.dto;

import java.util.UUID;

// Trimmed local copy of family-service's own FamilyDto - just the fields this service actually
// needs (chapter for scoping, head/mobile/area for report search). membership-service cannot
// import family-service's DTO class directly (separate deployables); every other service in this
// reactor that calls another service's REST API keeps its own local copy the same way (e.g.
// family-service's own BranchSummaryDto for user-service's chapters, not user-service's own DTO).
// Deserialized from family-service's GET /api/v1/families(/{id}) responses via FamilyClient -
// unknown JSON properties on the real response are ignored by default (Jackson).
public record FamilyDto(
        UUID id,
        String familyCode,
        UUID chapterId,
        String headOfFamilyName,
        String mobileNumber,
        String areaLocality
) {
}

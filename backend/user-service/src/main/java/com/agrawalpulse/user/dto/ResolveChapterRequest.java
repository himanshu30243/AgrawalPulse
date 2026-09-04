package com.agrawalpulse.user.dto;

import jakarta.validation.constraints.NotBlank;

// Resolves (or creates) the chapter matching a city+state - see ChapterResolutionRepository.
// Distinct from CreateChapterRequest: that's a deliberate admin action with a chosen name: this
// is an idempotent lookup any family/account editor can trigger just by changing their address.
public record ResolveChapterRequest(
        @NotBlank String city,
        @NotBlank String state
) {
}

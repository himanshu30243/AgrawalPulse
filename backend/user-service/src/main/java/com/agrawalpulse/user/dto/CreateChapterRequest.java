package com.agrawalpulse.user.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateChapterRequest(
        @NotBlank String name,
        @NotBlank String city,
        @NotBlank String state
) {
}

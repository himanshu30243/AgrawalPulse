package com.agrawalpulse.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateOwnChapterRequest(
        @NotNull UUID chapterId
) {
}

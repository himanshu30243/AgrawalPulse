package com.agrawalpulse.user.dto;

import java.time.Instant;
import java.util.UUID;

public record ChapterDto(
        UUID id,
        String name,
        String city,
        String state,
        Instant createdAt
) {
}

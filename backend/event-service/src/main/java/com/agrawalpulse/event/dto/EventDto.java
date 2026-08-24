package com.agrawalpulse.event.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EventDto(
        UUID id,
        UUID chapterId,
        String title,
        String description,
        LocalDate eventDate,
        String location
) {
}

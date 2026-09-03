package com.agrawalpulse.event.dto;

import com.agrawalpulse.event.entity.EventStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EventDto(
        UUID id,
        UUID chapterId,
        String chapterName,
        String title,
        String description,
        String category,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        String organizerName,
        String contactDetails,
        EventStatus status,
        boolean hasBanner,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt
) {
}

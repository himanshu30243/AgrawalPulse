package com.agrawalpulse.event.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

// Status is deliberately absent - a created event always starts DRAFT (see
// EventServiceImpl.createEvent); publishing/unpublishing/cancelling are their own explicit
// endpoints, never a field a caller sets directly on create/update.
public record CreateEventRequest(
        @NotBlank String title,
        String description,
        String category,
        @NotNull @FutureOrPresent LocalDate eventDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String location,
        String organizerName,
        String contactDetails
) {
}

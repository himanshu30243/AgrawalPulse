package com.agrawalpulse.event.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

// Same shape/validation as CreateEventRequest - "cannot be in the past" governs any write that
// sets/changes eventDate, not just creation. Status is still absent here for the same reason.
public record UpdateEventRequest(
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

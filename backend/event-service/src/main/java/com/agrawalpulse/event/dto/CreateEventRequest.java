package com.agrawalpulse.event.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateEventRequest(
        @NotBlank String title,
        String description,
        @NotNull @FutureOrPresent LocalDate eventDate,
        String location
) {
}

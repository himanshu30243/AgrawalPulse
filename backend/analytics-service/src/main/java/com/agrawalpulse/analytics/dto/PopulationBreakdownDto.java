package com.agrawalpulse.analytics.dto;

// Shared shape for both by-city and by-state breakdowns - `label` is a city name or a state name
// depending on which endpoint returned it, documented there rather than via two near-identical
// record types.
public record PopulationBreakdownDto(String label, long count) {
}

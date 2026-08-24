package com.agrawalpulse.analytics.dto;

import java.util.UUID;

public record PopulationCountDto(UUID chapterId, String gender, String city, long totalPopulation) {
}

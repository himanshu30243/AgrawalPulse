package com.agrawalpulse.analytics.dto;

import java.util.UUID;

public record ActiveMembershipCountDto(UUID chapterId, int year, long activeMemberships) {
}

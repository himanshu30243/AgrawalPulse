package com.agrawalpulse.analytics.dto;

import java.util.UUID;

// Aggregate counts only - per the requirements doc, readiness *counts* may be reported
// nationally/per-chapter even though individual matrimony profiles stay consent-gated
// (see matrimony-service). No name/PII is exposed here.
public record MarriageReadinessCountDto(UUID chapterId, long girlsReady, long boysReady, long otherReady) {
}

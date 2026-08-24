package com.agrawalpulse.family.service;

import java.util.UUID;

/**
 * How much of the families table the calling user may read, resolved once per request from their
 * JWT-derived tenant context and permission set (see FamilyController).
 *
 * <p>Permission-driven, not role-driven, matching this codebase's convention everywhere else
 * (RbacController's javadoc, permissions.ts) - a role granted these permissions at runtime gets
 * the matching scope with no code change here. The four tiers are mutually exclusive by precedence
 * (broadest wins): VIEW_ALL_FAMILIES > VIEW_STATE_FAMILIES > VIEW_CHAPTER_FAMILIES > own-record-only.
 */
public record FamilyAccessScope(UUID chapterId, UUID userId, boolean viewAll, boolean viewState, boolean viewChapter) {
}

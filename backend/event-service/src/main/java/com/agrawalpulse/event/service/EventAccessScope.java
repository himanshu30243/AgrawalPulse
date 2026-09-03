package com.agrawalpulse.event.service;

import java.util.UUID;

// How much of the events table the calling user may read/manage, resolved once per request from
// their JWT-derived tenant context and permission set (see EventController.resolveScope()).
// Permission-driven, matching FamilyAccessScope/MembershipAccessScope's shape: three tiers,
// broadest wins - VIEW_ALL_EVENTS > VIEW_STATE_EVENTS > VIEW_CHAPTER_EVENTS > (no tier) own chapter.
//
// Two differences from FamilyAccessScope: (1) userId is carried purely to stamp createdBy/
// updatedBy (mirrors MembershipAccessScope's identical justification), never for ownership
// filtering - events have no per-user ownership concept at all. (2) the viewChapter tier and the
// no-tier-permission floor are the SAME check (exact chapterId match) - unlike Family, there is no
// narrower "just what I own" fallback to drop to, since nobody owns an event; every role has
// always seen its whole own chapter's events, admin or not.
public record EventAccessScope(UUID chapterId, UUID userId, boolean viewAll, boolean viewState, boolean viewChapter) {
}

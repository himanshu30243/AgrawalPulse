package com.agrawalpulse.membership.service;

import java.util.UUID;

// Same shape as family-service's FamilyAccessScope (chapterId/userId + three broadening flags,
// broadest wins - VIEW_ALL_MEMBERSHIP > VIEW_STATE_MEMBERSHIP > VIEW_CHAPTER_MEMBERSHIP > own
// family only), but the *own-tier* resolution mechanism is fundamentally different: Membership has
// deliberately no local ownerUserId column (family-service already owns that data), so its own-tier
// instead delegates to FamilyClient.getFamily(familyId) - forwarding the caller's own JWT so
// family-service's own FamilyAccessScope (its ownerUserId check) is the actual authority on "is
// this the caller's own family." A 404 there is treated as "not the caller's membership" here too
// - see MembershipServiceImpl.isInScope.
public record MembershipAccessScope(UUID chapterId, UUID userId, boolean viewAll, boolean viewState,
                                     boolean viewChapter) {
}

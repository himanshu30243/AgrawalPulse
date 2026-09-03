package com.agrawalpulse.event.dto;

// Query-only filter for the browse/manage listings - not persisted, unlike EventStatus. Bound
// directly as a @RequestParam the same way MembershipController binds MembershipStatus.
public enum EventTimeframe {
    UPCOMING,
    PAST
}

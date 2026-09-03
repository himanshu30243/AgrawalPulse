package com.agrawalpulse.membership.entity;

// ACTIVE/PENDING_RENEWAL are the only values a Membership *row* ever stores (row-scoped to one
// FY - see Membership's class comment). EXPIRED is a family-level roll-up across every FY row a
// family has (2+ FYs unpaid, or never paid at all), computed at read time by
// MembershipServiceImpl.computeStatus - never persisted on a memberships row itself.
public enum MembershipStatus {
    ACTIVE,
    PENDING_RENEWAL,
    EXPIRED
}

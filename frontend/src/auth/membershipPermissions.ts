import { PERMISSIONS } from '@/types/domain';
import type { Permission } from '@/types/domain';
import { hasPermission } from './permissions';

// Membership-module authorization helpers. Mirrors auth/permissions.ts's family-module shape -
// see that file's header comment for why these take permission CODES, never role names.
//
// UX gating only. membership-service re-checks every one of these server-side (see
// MembershipController's @PreAuthorize table) - anything enforced here must also be enforced
// there.

export interface MembershipAccessContext {
  permissions: readonly Permission[];
}

/** Can record/edit membership transactions - the write side (requirement #2). */
export function canManageMembership({ permissions }: MembershipAccessContext): boolean {
  return hasPermission(permissions, PERMISSIONS.manageMembership);
}

/**
 * Can see the admin tools (Members list, Pending Payment Report, Collection Summary) - any tier
 * broader than "just my own family" per V6__membership_visibility_scopes.sql. A plain USER (only
 * VIEW_MEMBERSHIP) gets neither this nor canManageMembership and sees just their own status tab.
 */
export function canViewMembershipAdminTools({ permissions }: MembershipAccessContext): boolean {
  return (
    hasPermission(permissions, PERMISSIONS.manageMembership) ||
    hasPermission(permissions, PERMISSIONS.viewChapterMembership) ||
    hasPermission(permissions, PERMISSIONS.viewStateMembership) ||
    hasPermission(permissions, PERMISSIONS.viewAllMembership)
  );
}

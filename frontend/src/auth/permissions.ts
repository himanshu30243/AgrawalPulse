import { PERMISSIONS } from '@/types/domain';
import type { Family, Permission } from '@/types/domain';
import type { AuthUser } from './types';

// Family-module authorization helpers.
//
// These take permission CODES, never role names - the role -> permission mapping lives in
// user-service's role_permissions table and is delivered per-user by GET /api/v1/me. That is what
// lets an administrator create a role, grant it CREATE_FAMILY, and have this file work unchanged.
//
// UX gating only. family-service re-checks every one of these server-side; anything enforced here
// must also be enforced there (see FamilyServiceImpl's registration cap).

/**
 * Families a user may own without CREATE_FAMILY_UNLIMITED.
 * Mirrors FamilyServiceImpl.MAX_FAMILIES_PER_OWNER - keep the two in step.
 */
export const MAX_FAMILIES_PER_OWNER = 1;

export function hasPermission(permissions: readonly Permission[], required: Permission): boolean {
  return permissions.includes(required);
}

/**
 * Whether `family` belongs to `user`.
 *
 * Prefers the authoritative `ownerUserId` recorded by family-service. Falls back to matching the
 * head-of-family email for rows registered before that column existed, which is a heuristic: it
 * misses a family registered under a different contact address. Both branches live here so the
 * fallback can be deleted once legacy rows are backfilled.
 */
export function isOwnFamily(
  family: Family,
  user: Pick<AuthUser, 'id' | 'email'> | null,
): boolean {
  if (!user) return false;
  if (family.ownerUserId) {
    return family.ownerUserId === user.id;
  }
  if (!user.email || !family.email) return false;
  return family.email.trim().toLowerCase() === user.email.trim().toLowerCase();
}

export function countOwnedFamilies(
  families: readonly Family[],
  user: Pick<AuthUser, 'id' | 'email'> | null,
): number {
  return families.filter((family) => isOwnFamily(family, user)).length;
}

export interface FamilyAccessContext {
  permissions: readonly Permission[];
  /** Families already owned by this user - drives the per-user cap. Defaults to 0. */
  ownedFamilyCount?: number;
}

/** Why family creation is unavailable, or null when it is available. */
export type CreateFamilyDenial = 'no-permission' | 'limit-reached';

export function whyCannotCreateFamily({
  permissions,
  ownedFamilyCount = 0,
}: FamilyAccessContext): CreateFamilyDenial | null {
  if (!hasPermission(permissions, PERMISSIONS.createFamily)) return 'no-permission';
  if (hasPermission(permissions, PERMISSIONS.createFamilyUnlimited)) return null;
  return ownedFamilyCount >= MAX_FAMILIES_PER_OWNER ? 'limit-reached' : null;
}

export function canCreateFamily(context: FamilyAccessContext): boolean {
  return whyCannotCreateFamily(context) === null;
}

/** Any permission that grants edit/view rights over families beyond the caller's own record. */
function hasBroadFamilyScope(permissions: readonly Permission[]): boolean {
  return (
    hasPermission(permissions, PERMISSIONS.viewAllFamilies) ||
    hasPermission(permissions, PERMISSIONS.viewStateFamilies) ||
    hasPermission(permissions, PERMISSIONS.viewChapterFamilies)
  );
}

export function canEditFamily(
  { permissions }: FamilyAccessContext,
  family: Family,
  user: Pick<AuthUser, 'id' | 'email'> | null,
): boolean {
  if (!hasPermission(permissions, PERMISSIONS.editFamily)) return false;
  // EDIT_FAMILY plus any broader-than-own read scope (chapter/state/all) means edit rights over
  // every family that scope can see; with only VIEW_FAMILY it means editing the one you own.
  if (hasBroadFamilyScope(permissions)) return true;
  return isOwnFamily(family, user);
}

export function canDeleteFamily({ permissions }: FamilyAccessContext): boolean {
  return hasPermission(permissions, PERMISSIONS.deleteFamily);
}

export function canViewAllFamilies({ permissions }: FamilyAccessContext): boolean {
  return hasPermission(permissions, PERMISSIONS.viewAllFamilies);
}

/**
 * The family list to render, given what the API already returned.
 *
 * family-service is the authority on read scope (owner/chapter/state/all, resolved server-side
 * from the caller's permissions - see FamilyAccessScope): GET /families already returns exactly
 * the rows this user may see, nothing more. This must NOT re-narrow that result - it used to
 * (pre-owner-DTO-fix) fall back to matching a family's contact email against the login email when
 * ownerUserId was absent, which hid a user's own just-created family the moment those two emails
 * differed. Kept as a named pass-through (rather than inlined at call sites) so the "backend is
 * authoritative" invariant has one place to read and to guard with a comment.
 */
export function visibleFamilies(
  families: readonly Family[],
  _context: FamilyAccessContext,
  _user: Pick<AuthUser, 'id' | 'email'> | null,
): Family[] {
  return [...families];
}

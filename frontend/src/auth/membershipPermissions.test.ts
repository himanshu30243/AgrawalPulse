import { describe, expect, it } from 'vitest';
import { PERMISSIONS } from '@/types/domain';
import { canManageMembership, canViewMembershipAdminTools } from './membershipPermissions';

// Permission bundles standing in for what GET /me returns for each seeded role, mirroring
// user-service's V6__membership_visibility_scopes.sql exactly (see auth/permissions.test.ts for
// the same convention on the family side).
const USER = [PERMISSIONS.viewMembership];
const CHAPTER_ADMIN = [PERMISSIONS.viewMembership, PERMISSIONS.manageMembership, PERMISSIONS.viewChapterMembership];
const STATE_ADMIN = [PERMISSIONS.viewMembership, PERMISSIONS.manageMembership, PERMISSIONS.viewStateMembership];
const NATIONAL_ADMIN = [PERMISSIONS.viewMembership, PERMISSIONS.manageMembership, PERMISSIONS.viewAllMembership];

describe('canManageMembership', () => {
  it('requires MANAGE_MEMBERSHIP specifically - VIEW_MEMBERSHIP alone is not enough', () => {
    expect(canManageMembership({ permissions: USER })).toBe(false);
    expect(canManageMembership({ permissions: CHAPTER_ADMIN })).toBe(true);
  });
});

describe('canViewMembershipAdminTools', () => {
  it('is false for a plain member (own-status tab only)', () => {
    expect(canViewMembershipAdminTools({ permissions: USER })).toBe(false);
  });

  it('is true for any of the three visibility tiers', () => {
    expect(canViewMembershipAdminTools({ permissions: CHAPTER_ADMIN })).toBe(true);
    expect(canViewMembershipAdminTools({ permissions: STATE_ADMIN })).toBe(true);
    expect(canViewMembershipAdminTools({ permissions: NATIONAL_ADMIN })).toBe(true);
  });

  it('is true for MANAGE_MEMBERSHIP even without an explicit view tier', () => {
    expect(canViewMembershipAdminTools({ permissions: [PERMISSIONS.manageMembership] })).toBe(true);
  });

  it('works for a permission set naming no known role - the point of DB-driven roles', () => {
    const invented = [PERMISSIONS.viewMembership, PERMISSIONS.viewStateMembership];
    expect(canViewMembershipAdminTools({ permissions: invented })).toBe(true);
  });
});

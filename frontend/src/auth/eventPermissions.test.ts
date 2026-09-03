import { describe, expect, it } from 'vitest';
import { PERMISSIONS } from '@/types/domain';
import { canManageEvents } from './eventPermissions';

// Permission bundles standing in for what GET /me returns for each seeded role, mirroring
// user-service's V7__event_visibility_scopes.sql (see auth/membershipPermissions.test.ts for the
// same convention).
const USER = [PERMISSIONS.viewEvents];
const CHAPTER_ADMIN = [PERMISSIONS.viewEvents, PERMISSIONS.manageEvents, PERMISSIONS.viewChapterEvents];

describe('canManageEvents', () => {
  it('requires MANAGE_EVENTS specifically - VIEW_EVENTS alone is not enough', () => {
    expect(canManageEvents({ permissions: USER })).toBe(false);
    expect(canManageEvents({ permissions: CHAPTER_ADMIN })).toBe(true);
  });

  it('works for a permission set naming no known role - the point of DB-driven roles', () => {
    const invented = [PERMISSIONS.viewEvents, PERMISSIONS.manageEvents];
    expect(canManageEvents({ permissions: invented })).toBe(true);
  });
});

import { PERMISSIONS } from '@/types/domain';
import type { Permission } from '@/types/domain';
import { hasPermission } from './permissions';

// Event-module authorization helpers. Mirrors auth/membershipPermissions.ts's shape - see that
// file's header comment for why these take permission CODES, never role names.
//
// UX gating only. event-service re-checks every one of these server-side (see
// EventController's @PreAuthorize table) - anything enforced here must also be enforced there.

export interface EventAccessContext {
  permissions: readonly Permission[];
}

/** Can create/edit/delete/publish/unpublish/cancel events and upload banners. */
export function canManageEvents({ permissions }: EventAccessContext): boolean {
  return hasPermission(permissions, PERMISSIONS.manageEvents);
}

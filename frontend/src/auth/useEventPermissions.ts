import { useMemo } from 'react';
import { useAuth } from './useAuth';
import { canManageEvents } from './eventPermissions';

export interface EventPermissions {
  canManageEvents: boolean;
}

/** Event-module capabilities for the signed-in user. Mirrors useMembershipPermissions's shape. */
export function useEventPermissions(): EventPermissions {
  const { permissions } = useAuth();

  return useMemo(() => ({ canManageEvents: canManageEvents({ permissions }) }), [permissions]);
}

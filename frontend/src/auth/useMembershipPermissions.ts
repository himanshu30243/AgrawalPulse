import { useMemo } from 'react';
import { useAuth } from './useAuth';
import { canManageMembership, canViewMembershipAdminTools } from './membershipPermissions';

export interface MembershipPermissions {
  canManageMembership: boolean;
  canViewMembershipAdminTools: boolean;
}

/** Membership-module capabilities for the signed-in user. Mirrors useFamilyPermissions's shape. */
export function useMembershipPermissions(): MembershipPermissions {
  const { permissions } = useAuth();

  return useMemo(() => {
    const context = { permissions };
    return {
      canManageMembership: canManageMembership(context),
      canViewMembershipAdminTools: canViewMembershipAdminTools(context),
    };
  }, [permissions]);
}

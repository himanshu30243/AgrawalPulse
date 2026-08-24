import { useMemo } from 'react';
import type { Family } from '@/types/domain';
import { useAuth } from './useAuth';
import {
  canCreateFamily,
  canDeleteFamily,
  canEditFamily,
  canViewAllFamilies,
  countOwnedFamilies,
  visibleFamilies,
  whyCannotCreateFamily,
} from './permissions';
import type { CreateFamilyDenial } from './permissions';

export interface FamilyPermissions {
  /** Families the signed-in user owns, out of the list passed in. */
  ownedFamilyCount: number;
  canCreateFamily: boolean;
  /** Null when creation is allowed - otherwise why it isn't, for message selection. */
  createDenial: CreateFamilyDenial | null;
  canDeleteFamily: boolean;
  canViewAllFamilies: boolean;
  canEditFamily: (family: Family) => boolean;
  /** `families` narrowed to the user's read scope (own / chapter / all). */
  visibleFamilies: Family[];
}

const NO_FAMILIES: readonly Family[] = [];

/**
 * Family-module capabilities for the signed-in user.
 *
 * Pass the families already loaded by the caller (the list page has them anyway) - the per-user
 * creation cap needs to know how many the user owns, and threading the list in avoids a second
 * fetch. Callers that only need role-derived answers (e.g. a route guard, which has no list yet)
 * can omit it; only the cap depends on it.
 */
export function useFamilyPermissions(families: readonly Family[] = NO_FAMILIES): FamilyPermissions {
  const { user, permissions } = useAuth();

  return useMemo(() => {
    const ownedFamilyCount = countOwnedFamilies(families, user);
    const context = { permissions, ownedFamilyCount };

    return {
      ownedFamilyCount,
      canCreateFamily: canCreateFamily(context),
      createDenial: whyCannotCreateFamily(context),
      canDeleteFamily: canDeleteFamily(context),
      canViewAllFamilies: canViewAllFamilies(context),
      canEditFamily: (family: Family) => canEditFamily(context, family, user),
      visibleFamilies: visibleFamilies(families, context, user),
    };
  }, [families, user, permissions]);
}

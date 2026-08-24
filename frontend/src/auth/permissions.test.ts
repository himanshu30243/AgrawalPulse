import { describe, expect, it } from 'vitest';
import { mockFamilies } from '@/mocks/fixtures';
import { PERMISSIONS } from '@/types/domain';
import {
  MAX_FAMILIES_PER_OWNER,
  canCreateFamily,
  canDeleteFamily,
  canEditFamily,
  canViewAllFamilies,
  countOwnedFamilies,
  isOwnFamily,
  visibleFamilies,
  whyCannotCreateFamily,
} from './permissions';
import type { Family } from '@/types/domain';

const [familyOne, familyTwo] = mockFamilies;
if (!familyOne || !familyTwo) throw new Error('fixture data missing');

// Permission bundles standing in for what GET /me returns for each seeded role. Named after the
// role only for readability - nothing in the code under test knows these names exist.
const USER = [
  PERMISSIONS.viewFamily,
  PERMISSIONS.createFamily,
  PERMISSIONS.editFamily,
];
const CHAPTER_ADMIN = [
  PERMISSIONS.viewFamily,
  PERMISSIONS.viewAllFamilies,
  PERMISSIONS.createFamily,
  PERMISSIONS.createFamilyUnlimited,
  PERMISSIONS.editFamily,
];
const ADMIN = [...CHAPTER_ADMIN, PERMISSIONS.deleteFamily];
const READ_ONLY = [PERMISSIONS.viewFamily, PERMISSIONS.viewAllFamilies];

const owner = { id: 'owner-1', email: familyOne.email };
const stranger = { id: 'stranger-1', email: 'stranger@example.com' };

describe('canCreateFamily', () => {
  it('allows a plain user their first family and no more', () => {
    expect(canCreateFamily({ permissions: USER, ownedFamilyCount: 0 })).toBe(true);
    expect(canCreateFamily({ permissions: USER, ownedFamilyCount: MAX_FAMILIES_PER_OWNER })).toBe(false);
  });

  it('does not cap holders of CREATE_FAMILY_UNLIMITED', () => {
    expect(canCreateFamily({ permissions: CHAPTER_ADMIN, ownedFamilyCount: 99 })).toBe(true);
  });

  it('refuses anyone without CREATE_FAMILY regardless of count', () => {
    expect(canCreateFamily({ permissions: READ_ONLY, ownedFamilyCount: 0 })).toBe(false);
    expect(canCreateFamily({ permissions: [], ownedFamilyCount: 0 })).toBe(false);
  });

  it('distinguishes "not allowed" from "at your limit" so the UI can explain itself', () => {
    expect(whyCannotCreateFamily({ permissions: READ_ONLY })).toBe('no-permission');
    expect(whyCannotCreateFamily({ permissions: USER, ownedFamilyCount: 1 })).toBe('limit-reached');
    expect(whyCannotCreateFamily({ permissions: USER, ownedFamilyCount: 0 })).toBeNull();
  });

  it('works for a permission set that names no known role - the point of DB-driven roles', () => {
    // A role invented at runtime and granted only CREATE_FAMILY behaves correctly with no code
    // change here.
    const invented = [PERMISSIONS.createFamily];
    expect(canCreateFamily({ permissions: invented, ownedFamilyCount: 0 })).toBe(true);
    expect(canCreateFamily({ permissions: invented, ownedFamilyCount: 1 })).toBe(false);
  });
});

describe('ownership', () => {
  it('prefers the authoritative ownerUserId when present', () => {
    const owned: Family = { ...familyOne, ownerUserId: 'owner-1', email: 'someone-else@example.com' };
    expect(isOwnFamily(owned, owner)).toBe(true);
    expect(isOwnFamily(owned, stranger)).toBe(false);
  });

  it('falls back to head-of-family email for rows with no recorded owner', () => {
    const legacy: Family = { ...familyOne, ownerUserId: null };
    expect(isOwnFamily(legacy, owner)).toBe(true);
    expect(isOwnFamily(legacy, { id: 'x', email: `  ${familyOne.email.toUpperCase()} ` })).toBe(true);
    expect(isOwnFamily(legacy, stranger)).toBe(false);
  });

  it('never matches on two blank emails', () => {
    const legacyBlank: Family = { ...familyTwo, ownerUserId: null };
    expect(legacyBlank.email).toBe('');
    expect(isOwnFamily(legacyBlank, { id: 'x', email: '' })).toBe(false);
    expect(isOwnFamily(legacyBlank, null)).toBe(false);
  });

  it('counts only the families the user owns', () => {
    expect(countOwnedFamilies(mockFamilies, owner)).toBe(1);
    expect(countOwnedFamilies(mockFamilies, stranger)).toBe(0);
  });
});

describe('edit / delete', () => {
  it('lets a plain user edit only their own family', () => {
    expect(canEditFamily({ permissions: USER }, familyOne, owner)).toBe(true);
    expect(canEditFamily({ permissions: USER }, familyTwo, owner)).toBe(false);
  });

  it('lets a chapter admin edit anyone in view', () => {
    expect(canEditFamily({ permissions: CHAPTER_ADMIN }, familyTwo, stranger)).toBe(true);
  });

  it('refuses edit without EDIT_FAMILY even over your own family', () => {
    expect(canEditFamily({ permissions: READ_ONLY }, familyOne, owner)).toBe(false);
  });

  it('reserves deletion for DELETE_FAMILY holders', () => {
    expect(canDeleteFamily({ permissions: ADMIN })).toBe(true);
    expect(canDeleteFamily({ permissions: CHAPTER_ADMIN })).toBe(false);
    expect(canDeleteFamily({ permissions: USER })).toBe(false);
  });
});

describe('visibleFamilies', () => {
  it('shows everything to VIEW_ALL_FAMILIES holders', () => {
    expect(canViewAllFamilies({ permissions: CHAPTER_ADMIN })).toBe(true);
    expect(visibleFamilies(mockFamilies, { permissions: CHAPTER_ADMIN }, stranger))
      .toHaveLength(mockFamilies.length);
  });

  it('limits a plain user to the family they own', () => {
    expect(visibleFamilies(mockFamilies, { permissions: USER }, owner).map((f) => f.id))
      .toEqual([familyOne.id]);
  });

  it('shows nothing without any family read permission', () => {
    expect(visibleFamilies(mockFamilies, { permissions: [] }, owner)).toEqual([]);
  });
});

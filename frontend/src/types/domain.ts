/**
 * A role code. Deliberately `string`, not a union: roles live in user-service's `roles` table and
 * an administrator can add one at runtime, so pinning the type to today's seeded set would mean a
 * new role is unrepresentable in the frontend - exactly the coupling the RBAC migration removed.
 *
 * {@link SEEDED_ROLES} lists what V2 seeds, for reference and test fixtures only. Do not branch on
 * a role code to decide what a user may do - use permissions ({@link Permission}) instead.
 */
export type Role = string;

/** The roles seeded by user-service's V2 migration. Reference only - never an exhaustive list. */
export const SEEDED_ROLES = ['USER', 'CHAPTER_ADMIN', 'STATE_ADMIN', 'NATIONAL_ADMIN', 'ADMIN'] as const;

/**
 * A permission code as issued by the backend (bare, without the JWT's PERM_ prefix). Also `string`
 * for the same reason as Role - permissions are rows, not code.
 */
export type Permission = string;

/** Permission codes seeded by V2, for autocomplete and to keep call sites greppable. */
export const PERMISSIONS = {
  viewDashboard: 'VIEW_DASHBOARD',
  viewFamily: 'VIEW_FAMILY',
  viewChapterFamilies: 'VIEW_CHAPTER_FAMILIES',
  viewStateFamilies: 'VIEW_STATE_FAMILIES',
  viewAllFamilies: 'VIEW_ALL_FAMILIES',
  createFamily: 'CREATE_FAMILY',
  createFamilyUnlimited: 'CREATE_FAMILY_UNLIMITED',
  editFamily: 'EDIT_FAMILY',
  deleteFamily: 'DELETE_FAMILY',
  viewMembership: 'VIEW_MEMBERSHIP',
  manageMembership: 'MANAGE_MEMBERSHIP',
  viewEvents: 'VIEW_EVENTS',
  manageEvents: 'MANAGE_EVENTS',
  viewMatrimonyDirectory: 'VIEW_MATRIMONY_DIRECTORY',
  manageMatrimonyConsent: 'MANAGE_MATRIMONY_CONSENT',
  viewReports: 'VIEW_REPORTS',
  manageUsers: 'MANAGE_USERS',
  manageRoles: 'MANAGE_ROLES',
  manageBranches: 'MANAGE_BRANCHES',
} as const;

/** A navigation entry, as returned by GET /api/v1/me. Flat; `parentMenuKey` implies the tree. */
export interface MenuItem {
  menuId: string;
  menuKey: string;
  menuName: string;
  menuPath: string | null;
  icon: string | null;
  displayOrder: number;
  parentMenuKey: string | null;
  active: boolean;
}

export interface RoleSummary {
  roleId: string;
  roleCode: Role;
  roleName: string;
}

/** GET /api/v1/me - identity plus everything needed to render the shell for this user. */
export interface MeResponse {
  userId: string;
  email: string;
  chapterId: string;
  role: RoleSummary;
  menus: MenuItem[];
  permissions: Permission[];
}

export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type MaritalStatus = 'SINGLE' | 'MARRIED' | 'WIDOWED' | 'DIVORCED';
export type RelationshipToHead =
  | 'SELF'
  | 'SPOUSE'
  | 'SON'
  | 'DAUGHTER'
  | 'FATHER'
  | 'MOTHER'
  | 'BROTHER'
  | 'SISTER'
  | 'OTHER';
// Matches backend/family-service's FamilyCategory enum - values per
// frontend/docs/family-registration.md's Step 4 "Family Category" dropdown (replaces the earlier
// income-bracket-based BPL/MIDDLE_CLASS/UPPER_MIDDLE_CLASS/HIGH_INCOME set).
export type FamilyCategory = 'BUSINESS' | 'SALARIED' | 'PROFESSIONAL' | 'RETIRED' | 'AGRICULTURE' | 'OTHER';
// Matches backend/family-service's Samaj enum - Step 3 "Samaj" dropdown.
export type Samaj = 'AGRAWAL' | 'OTHER';
export type BloodGroup =
  | 'A_POSITIVE'
  | 'A_NEGATIVE'
  | 'B_POSITIVE'
  | 'B_NEGATIVE'
  | 'AB_POSITIVE'
  | 'AB_NEGATIVE'
  | 'O_POSITIVE'
  | 'O_NEGATIVE';

// "Branch" is the display-layer name for what user-service still calls a Chapter internally
// (entity/table/API path unchanged there - see backend/family-service's BranchSummaryDto).
export interface BranchSummary {
  id: string;
  name: string;
  city: string;
  state: string;
}

export interface FamilyMember {
  id: string;
  familyId: string;
  name: string;
  relationshipToHead: RelationshipToHead | null;
  dateOfBirth: string;
  age: number;
  gender: Gender;
  maritalStatus: MaritalStatus;
  education: string | null;
  instituteName: string | null;
  profession: string | null;
  companyName: string | null;
  mobileNumber: string | null;
  email: string | null;
  bloodGroup: BloodGroup | null;
}

export interface CreateFamilyMemberRequest {
  name: string;
  relationshipToHead: RelationshipToHead | null;
  dateOfBirth: string;
  gender: Gender;
  maritalStatus: MaritalStatus | null;
  education: string;
  instituteName: string;
  profession: string;
  companyName: string;
  mobileNumber: string;
  email: string;
  bloodGroup: BloodGroup | null;
}

export interface Family {
  id: string;
  familyCode: string;
  chapterId: string;
  branch: BranchSummary | null;
  headFirstName: string;
  headMiddleName: string | null;
  headLastName: string;
  // Server-computed concatenation of the three fields above - read-only, never sent on create.
  headOfFamilyName: string;
  familyName: string;
  headGender: Gender | null;
  headDateOfBirth: string;
  mobileNumber: string;
  email: string;
  aadhaarNumber: string | null;
  address: string;
  district: string;
  country: string;
  state: string;
  // Server-derived from district (read-only) - see CreateFamilyRequest's comment.
  city: string;
  areaLocality: string;
  pinCode: string;
  samaj: Samaj | null;
  gotra: string;
  nativePlace: string;
  occupationBusinessType: string;
  annualIncomeRange: string;
  familyCategory: FamilyCategory | null;
  ownTwoWheeler: boolean;
  ownFourWheeler: boolean;
  ownHome: boolean;
  ownPlot: boolean;
  willingToContribute: boolean | null;
  // Computed at read time by the backend (checks photo storage) - never itself uploaded/sent.
  hasProfilePhoto: boolean;
  // The user who registered this family. Null for rows created before family-service's
  // V2__family_owner.sql, which are treated as unowned.
  ownerUserId: string | null;
  createdAt: string;
}

// Field set and required-ness mirror frontend/docs/family-registration.md's wizard steps 1-4.
// city is deliberately absent - it's server-derived from district (auto-populated, read-only per
// the spec's "Region / City" field), so there's nothing for the client to send for it.
export interface CreateFamilyRequest {
  headFirstName: string;
  headMiddleName: string;
  headLastName: string;
  familyName: string;
  headGender: Gender | null;
  headDateOfBirth: string;
  mobileNumber: string;
  email: string;
  aadhaarNumber: string;
  address: string;
  country: string;
  state: string;
  district: string;
  areaLocality: string;
  pinCode: string;
  samaj: Samaj | null;
  gotra: string;
  nativePlace: string;
  occupationBusinessType: string;
  annualIncomeRange: string;
  familyCategory: FamilyCategory | null;
  ownTwoWheeler: boolean;
  ownFourWheeler: boolean;
  ownHome: boolean;
  ownPlot: boolean;
  willingToContribute: boolean | null;
}

export type MembershipStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';

export interface MembershipSummary {
  familyId: string;
  status: MembershipStatus;
  annualFee: number;
  lastPaymentDate: string | null;
  nextDueDate: string | null;
}

export interface MembershipPayment {
  id: string;
  familyId: string;
  amount: number;
  paymentDate: string;
  receiptNo: string;
}

export interface ChapterCollectionSummary {
  chapterId: string;
  chapterName: string;
  totalCollected: number;
  familiesPaid: number;
  familiesPending: number;
}

export type ConsentScope = 'CHAPTER' | 'NATIONAL';

export interface MatrimonyConsent {
  individualId: string;
  fullName: string;
  consentGiven: boolean;
  consentScope: ConsentScope | null;
  consentGivenBy: string | null;
  consentGivenAt: string | null;
}

export interface MatrimonyProfile {
  individualId: string;
  fullName: string;
  chapterName: string;
  district: string;
  age: number;
  education: string;
  profession: string;
  consentScope: ConsentScope;
}

export interface EventItem {
  id: string;
  name: string;
  eventDate: string;
  location: string;
  description: string;
  chapterName: string;
  capacity: number;
  registeredCount: number;
}

export interface EventRegistrationRequest {
  eventId: string;
  attendeeCount: number;
}

export interface FamiliesByChapterDatum {
  chapter: string;
  families: number;
}

export interface MembershipStatusDatum {
  status: MembershipStatus;
  count: number;
}

export interface AgeEligibilityDatum {
  ageBand: string;
  boys: number;
  girls: number;
}

export interface AnalyticsSummary {
  totalFamilies: number;
  totalPopulation: number;
  activeMembers: number;
  membershipCollection: number;
  eligibleBoys: number;
  eligibleGirls: number;
  familiesByChapter: FamiliesByChapterDatum[];
  membershipStatusSplit: MembershipStatusDatum[];
  ageEligibilityDistribution: AgeEligibilityDatum[];
}

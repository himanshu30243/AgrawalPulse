import type { BranchSummary, Family, FamilyMember } from '@/types/domain';
import type { LocalLoginResponse } from '@/auth/types';

// Realistic dummy data for standalone GUI testing/development - shaped exactly like the real
// backend DTOs (see backend/family-service, backend/user-service). Used by both the MSW mock
// handlers (src/mocks/handlers.ts, for `npm run dev:mock` and component tests) and can be
// imported directly in tests that don't need the network layer at all.

export const mockBranches: BranchSummary[] = [
  { id: 'branch-indore', name: 'Indore Chapter', city: 'Indore', state: 'Madhya Pradesh' },
  { id: 'branch-bhopal', name: 'Bhopal Chapter', city: 'Bhopal', state: 'Madhya Pradesh' },
  { id: 'branch-ujjain', name: 'Ujjain Chapter', city: 'Ujjain', state: 'Madhya Pradesh' },
];

export const mockFamilyMembers: Record<string, FamilyMember[]> = {
  'family-1': [
    {
      id: 'member-1',
      familyId: 'family-1',
      name: 'Ramesh Agrawal',
      relationshipToHead: 'SELF',
      dateOfBirth: '1975-03-14',
      age: 51,
      gender: 'MALE',
      maritalStatus: 'MARRIED',
      education: 'B.Com',
      instituteName: null,
      profession: 'Businessman',
      companyName: 'Agrawal Textiles',
      mobileNumber: '9876543210',
      email: 'ramesh.agrawal@example.com',
      bloodGroup: 'B_POSITIVE',
    },
    {
      id: 'member-2',
      familyId: 'family-1',
      name: 'Suman Agrawal',
      relationshipToHead: 'SPOUSE',
      dateOfBirth: '1980-07-22',
      age: 46,
      gender: 'FEMALE',
      maritalStatus: 'MARRIED',
      education: 'M.A.',
      instituteName: null,
      profession: 'Homemaker',
      companyName: null,
      mobileNumber: null,
      email: null,
      bloodGroup: 'O_POSITIVE',
    },
    {
      id: 'member-3',
      familyId: 'family-1',
      name: 'Priya Agrawal',
      relationshipToHead: 'DAUGHTER',
      dateOfBirth: '2002-11-05',
      age: 23,
      gender: 'FEMALE',
      maritalStatus: 'SINGLE',
      education: 'B.Tech',
      instituteName: 'IIT Indore',
      profession: 'Software Engineer',
      companyName: 'TCS',
      mobileNumber: '9998887777',
      email: 'priya.agrawal@example.com',
      bloodGroup: 'B_POSITIVE',
    },
  ],
  'family-2': [
    {
      id: 'member-4',
      familyId: 'family-2',
      name: 'Manoj Goyal',
      relationshipToHead: 'SELF',
      dateOfBirth: '1968-01-30',
      age: 58,
      gender: 'MALE',
      maritalStatus: 'MARRIED',
      education: 'B.Sc',
      instituteName: null,
      profession: 'Retired Teacher',
      companyName: null,
      mobileNumber: '9123456780',
      email: null,
      bloodGroup: 'A_POSITIVE',
    },
  ],
};

export const mockFamilies: Family[] = [
  {
    id: 'family-1',
    familyCode: 'FAM-DEMO0001',
    chapterId: 'branch-indore',
    branch: mockBranches[0] ?? null,
    headFirstName: 'Ramesh',
    headMiddleName: '',
    headLastName: 'Agrawal',
    headOfFamilyName: 'Ramesh Agrawal',
    familyName: 'Agrawal',
    headGender: 'MALE',
    headDateOfBirth: '1975-03-14',
    mobileNumber: '9876543210',
    email: 'ramesh.agrawal@example.com',
    aadhaarNumber: null,
    address: '12 MG Road',
    district: 'Indore',
    country: 'India',
    state: 'Madhya Pradesh',
    city: 'Indore',
    areaLocality: 'Vijay Nagar',
    pinCode: '452010',
    samaj: 'AGRAWAL',
    gotra: 'Garg',
    nativePlace: 'Ujjain',
    occupationBusinessType: 'Textile Business',
    annualIncomeRange: '₹10-25 Lakh',
    familyCategory: 'BUSINESS',
    ownTwoWheeler: true,
    ownFourWheeler: true,
    ownHome: true,
    ownPlot: false,
    willingToContribute: true,
    hasProfilePhoto: false,
    ownerUserId: null,
    createdAt: '2026-01-15T09:30:00Z',
  },
  {
    id: 'family-2',
    familyCode: 'FAM-DEMO0002',
    chapterId: 'branch-indore',
    branch: mockBranches[0] ?? null,
    headFirstName: 'Manoj',
    headMiddleName: '',
    headLastName: 'Goyal',
    headOfFamilyName: 'Manoj Goyal',
    familyName: 'Goyal',
    headGender: 'MALE',
    headDateOfBirth: '1968-01-30',
    mobileNumber: '9123456780',
    email: '',
    aadhaarNumber: null,
    address: '45 Ring Road',
    district: 'Indore',
    country: 'India',
    state: 'Madhya Pradesh',
    city: 'Indore',
    areaLocality: 'Palasia',
    pinCode: '452001',
    samaj: 'AGRAWAL',
    gotra: 'Mangal',
    nativePlace: 'Indore',
    occupationBusinessType: '',
    annualIncomeRange: '',
    familyCategory: 'RETIRED',
    ownTwoWheeler: false,
    ownFourWheeler: false,
    ownHome: true,
    ownPlot: false,
    willingToContribute: false,
    hasProfilePhoto: false,
    ownerUserId: null,
    createdAt: '2026-02-03T11:00:00Z',
  },
];

export function mockLoginResponse(): LocalLoginResponse {
  return {
    accessToken: buildMockJwt(),
    tokenType: 'Bearer',
    expiresInSeconds: 7200,
  };
}

// A structurally valid (but unsigned/fake-signed) JWT so jwt-decode can read it in tests and
// mock-mode - never validated against a real key, this never talks to a real backend.
function buildMockJwt(): string {
  const header = base64Url(JSON.stringify({ alg: 'none', typ: 'JWT' }));
  const payload = base64Url(
    JSON.stringify({
      sub: 'mock-user-1',
      email: 'demo@example.com',
      chapter_id: 'branch-indore',
      roles: ['CHAPTER_ADMIN', 'MATRIMONY_VIEWER'],
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 7200,
    }),
  );
  return `${header}.${payload}.mock-signature`;
}

function base64Url(value: string): string {
  return btoa(value).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

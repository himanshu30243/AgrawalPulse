import { http, HttpResponse } from 'msw';
import { mockBranches, mockEvents, mockFamilies, mockFamilyMembers } from './fixtures';
import { isOwnFamily } from '@/auth/permissions';
import type {
  CreateEventRequest,
  CreateFamilyMemberRequest,
  CreateFamilyRequest,
  EventItem,
  EventRegistration,
  EventStatus,
  Family,
  FamilyMember,
  MembershipStatus,
  MembershipStatusSummary,
  MembershipTransaction,
  RecordTransactionRequest,
  RegisterEventRequest,
  UpdateTransactionRequest,
} from '@/types/domain';

// Realistic in-memory mock backend for `npm run dev:mock` and every component test (see
// src/test/setup.ts). Paths here MUST include the "/api/v1" prefix - apiClient's baseURL
// (src/api/axiosClient.ts, driven by VITE_API_BASE_URL) is "/api/v1", so every real request is
// "/api/v1/<resource>". A handler registered without that prefix silently never matches and MSW
// falls through to the real network (server.listen({ onUnhandledRequest: 'error' }) turns that
// into a hard test failure; in the browser it just looks like "login failed"/network errors
// since nothing is listening on that port).

export const MOCK_USER_ID = 'mock-user-1';

// Mirrors the role -> menu and role -> permission seeding in user-service's
// V2__rbac_roles_permissions_menus.sql. Kept in step with it by hand; if the two drift, mock mode
// shows a different dashboard than the real backend does.
const MOCK_MENUS = [
  { menuId: 'm-dashboard', menuKey: 'dashboard', menuName: 'Dashboard', menuPath: '/dashboard', icon: 'Dashboard', displayOrder: 10, parentMenuKey: null, active: true },
  { menuId: 'm-families', menuKey: 'families', menuName: 'Family', menuPath: '/families', icon: 'FamilyRestroom', displayOrder: 20, parentMenuKey: null, active: true },
  { menuId: 'm-membership', menuKey: 'membership', menuName: 'Membership', menuPath: '/membership', icon: 'CardMembership', displayOrder: 30, parentMenuKey: null, active: true },
  { menuId: 'm-events', menuKey: 'events', menuName: 'Events', menuPath: '/events', icon: 'Event', displayOrder: 40, parentMenuKey: null, active: true },
  { menuId: 'm-matrimony', menuKey: 'matrimony', menuName: 'Matrimony', menuPath: '/matrimony/consent', icon: 'Favorite', displayOrder: 50, parentMenuKey: null, active: true },
  { menuId: 'm-reports', menuKey: 'reports', menuName: 'Reports', menuPath: '/reports', icon: 'Assessment', displayOrder: 60, parentMenuKey: null, active: true },
  { menuId: 'm-users', menuKey: 'userManagement', menuName: 'User Management', menuPath: '/admin/users', icon: 'People', displayOrder: 70, parentMenuKey: null, active: true },
  { menuId: 'm-branches', menuKey: 'branchManagement', menuName: 'Branch Management', menuPath: '/admin/branches', icon: 'AccountTree', displayOrder: 80, parentMenuKey: null, active: true },
  { menuId: 'm-roles', menuKey: 'roleManagement', menuName: 'Role Management', menuPath: '/admin/roles', icon: 'AdminPanelSettings', displayOrder: 90, parentMenuKey: null, active: true },
  { menuId: 'm-settings', menuKey: 'settings', menuName: 'Settings', menuPath: '/settings', icon: 'Settings', displayOrder: 100, parentMenuKey: null, active: true },
];

// Family-read scope tiers (VIEW_CHAPTER_FAMILIES/VIEW_STATE_FAMILIES/VIEW_ALL_FAMILIES) mirror
// user-service's V3/V4 migrations exactly - CHAPTER_ADMIN and STATE_ADMIN each hold only their own
// tier, never VIEW_ALL_FAMILIES (V4 fixed CHAPTER_ADMIN's over-broad V2 grant; V3 did the same for
// STATE_ADMIN). Keep this in step with those migrations by hand, same as MOCK_MENUS above.
const USER_PERMISSIONS = ['VIEW_DASHBOARD', 'VIEW_FAMILY', 'CREATE_FAMILY', 'EDIT_FAMILY', 'VIEW_MEMBERSHIP', 'VIEW_EVENTS', 'MANAGE_MATRIMONY_CONSENT'];
// VIEW_CHAPTER_MEMBERSHIP/VIEW_STATE_MEMBERSHIP/VIEW_ALL_MEMBERSHIP mirror user-service's
// V6__membership_visibility_scopes.sql exactly, same as the VIEW_x_FAMILIES tiers above mirror
// V3/V4 - kept in step with that migration by hand.
// VIEW_CHAPTER_EVENTS/VIEW_STATE_EVENTS/VIEW_ALL_EVENTS mirror user-service's
// V7__event_visibility_scopes.sql exactly, same as the VIEW_x_MEMBERSHIP tiers mirror V6 - kept
// in step with that migration by hand.
const CHAPTER_ADMIN_PERMISSIONS = [...USER_PERMISSIONS, 'VIEW_CHAPTER_FAMILIES', 'CREATE_FAMILY_UNLIMITED', 'MANAGE_MEMBERSHIP', 'VIEW_CHAPTER_MEMBERSHIP', 'MANAGE_EVENTS', 'VIEW_CHAPTER_EVENTS', 'VIEW_MATRIMONY_DIRECTORY', 'VIEW_REPORTS', 'MANAGE_BRANCHES'];
const STATE_ADMIN_PERMISSIONS = [...USER_PERMISSIONS, 'VIEW_STATE_FAMILIES', 'CREATE_FAMILY_UNLIMITED', 'MANAGE_MEMBERSHIP', 'VIEW_STATE_MEMBERSHIP', 'MANAGE_EVENTS', 'VIEW_STATE_EVENTS', 'VIEW_MATRIMONY_DIRECTORY', 'VIEW_REPORTS', 'MANAGE_BRANCHES', 'MANAGE_USERS'];
const NATIONAL_ADMIN_PERMISSIONS = [...USER_PERMISSIONS, 'VIEW_ALL_FAMILIES', 'CREATE_FAMILY_UNLIMITED', 'MANAGE_MEMBERSHIP', 'VIEW_ALL_MEMBERSHIP', 'MANAGE_EVENTS', 'VIEW_ALL_EVENTS', 'VIEW_MATRIMONY_DIRECTORY', 'VIEW_REPORTS', 'MANAGE_BRANCHES', 'MANAGE_USERS'];
const ADMIN_PERMISSIONS = [...NATIONAL_ADMIN_PERMISSIONS, 'VIEW_CHAPTER_FAMILIES', 'VIEW_STATE_FAMILIES', 'VIEW_CHAPTER_MEMBERSHIP', 'VIEW_STATE_MEMBERSHIP', 'VIEW_CHAPTER_EVENTS', 'VIEW_STATE_EVENTS', 'DELETE_FAMILY', 'MANAGE_ROLES'];

const MOCK_ROLE_PERMISSIONS: Record<string, string[]> = {
  USER: USER_PERMISSIONS,
  CHAPTER_ADMIN: CHAPTER_ADMIN_PERMISSIONS,
  STATE_ADMIN: STATE_ADMIN_PERMISSIONS,
  NATIONAL_ADMIN: NATIONAL_ADMIN_PERMISSIONS,
  ADMIN: ADMIN_PERMISSIONS,
  // Not one of the 5 seeded roles - stands in for a role an administrator creates at runtime
  // (FamiliesListPage.test.tsx's "never offers registration ..." case). The /families handler
  // below re-derives permissions from the JWT's role claim independently of whatever a test
  // overrides /me to return, so this entry has to be kept in step with that test's /me override
  // by hand, the same way the rest of this table is kept in step with the real seed migration.
  AUDITOR: ['VIEW_FAMILY', 'VIEW_ALL_FAMILIES'],
};

const MOCK_ROLE_MENUS: Record<string, string[]> = {
  USER: ['dashboard', 'families', 'membership', 'events'],
  CHAPTER_ADMIN: ['dashboard', 'families', 'membership', 'events', 'matrimony', 'reports', 'branchManagement'],
  STATE_ADMIN: ['dashboard', 'families', 'membership', 'events', 'matrimony', 'reports', 'branchManagement', 'userManagement'],
  NATIONAL_ADMIN: ['dashboard', 'families', 'membership', 'events', 'matrimony', 'reports', 'branchManagement', 'userManagement'],
  ADMIN: MOCK_MENUS.map((menu) => menu.menuKey),
};

interface DecodedMockToken {
  sub: string;
  email: string;
  chapterId: string;
  role: string;
}

// Reads identity out of the bearer token the caller already holds, so handlers reflect whoever
// signed in rather than a fixed identity - switching roles on the login screen (or a test's
// seedAuthUser) changes what /me and /families return, mirroring CurrentTenantResolver.
function decodeMockToken(header: string | null): DecodedMockToken {
  const fallback: DecodedMockToken = {
    sub: 'test-user',
    email: 'test-user@example.com',
    chapterId: 'branch-indore',
    role: 'USER',
  };
  if (!header?.startsWith('Bearer ')) return fallback;
  try {
    const payload = header.slice('Bearer '.length).split('.')[1];
    if (!payload) return fallback;
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as {
      sub?: string;
      email?: string;
      chapter_id?: string;
      roles?: string[];
    };
    return {
      sub: decoded.sub ?? fallback.sub,
      email: decoded.email ?? fallback.email,
      chapterId: decoded.chapter_id ?? fallback.chapterId,
      role: decoded.roles?.[0] ?? fallback.role,
    };
  } catch {
    return fallback;
  }
}

function roleFromAuthHeader(header: string | null): string {
  return decodeMockToken(header).role;
}

// Mutable admin fixtures - the admin screens mutate these, and each test file gets a fresh module
// instance, so changes don't leak between test files.
interface AdminRoleFixture {
  roleId: string;
  roleCode: string;
  roleName: string;
  description: string | null;
  active: boolean;
  permissionCodes: string[];
  menuKeys: string[];
  createdAt: string;
  updatedAt: string;
}

const adminRoles: AdminRoleFixture[] = [
  { roleId: 'role-USER', roleCode: 'USER', roleName: 'User', description: 'Self-registered member', active: true, permissionCodes: USER_PERMISSIONS, menuKeys: ['dashboard', 'families', 'membership', 'events'], createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { roleId: 'role-CHAPTER_ADMIN', roleCode: 'CHAPTER_ADMIN', roleName: 'Chapter Admin', description: 'Manages one chapter', active: true, permissionCodes: CHAPTER_ADMIN_PERMISSIONS, menuKeys: ['dashboard', 'families', 'membership', 'events', 'matrimony', 'reports', 'branchManagement'], createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { roleId: 'role-STATE_ADMIN', roleCode: 'STATE_ADMIN', roleName: 'State Admin', description: null, active: true, permissionCodes: STATE_ADMIN_PERMISSIONS, menuKeys: ['dashboard', 'families', 'reports'], createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { roleId: 'role-NATIONAL_ADMIN', roleCode: 'NATIONAL_ADMIN', roleName: 'National Admin', description: null, active: true, permissionCodes: STATE_ADMIN_PERMISSIONS, menuKeys: ['dashboard', 'families', 'reports'], createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { roleId: 'role-ADMIN', roleCode: 'ADMIN', roleName: 'Administrator', description: 'Full access', active: true, permissionCodes: ADMIN_PERMISSIONS, menuKeys: MOCK_MENUS.map((m) => m.menuKey), createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
];

// Mutable chapter list so create/update in Branch Management persist for the session.
const chapters: Array<{ id: string; name: string; city: string; state: string; createdAt?: string }> =
  mockBranches.map((branch) => ({ ...branch, createdAt: '2026-01-01T00:00:00Z' }));

const adminUsers = [
  { id: 'user-1', chapterId: 'branch-indore', firstName: 'Ramesh', middleName: null, lastName: 'Agrawal', dateOfBirth: '1985-04-12', gender: 'MALE' as const, mobileNumber: '9876500001', email: 'ramesh.agrawal@example.com', cognitoSub: null, status: 'ACTIVE' as const, role: { roleId: 'role-USER', roleCode: 'USER', roleName: 'User' }, createdAt: '2026-01-15T09:30:00Z', updatedAt: '2026-01-15T09:30:00Z' },
  { id: 'user-2', chapterId: 'branch-indore', firstName: 'Priya', middleName: null, lastName: 'Sharma', dateOfBirth: '1980-01-01', gender: 'FEMALE' as const, mobileNumber: '9876500002', email: 'chapter.admin@example.com', cognitoSub: null, status: 'ACTIVE' as const, role: { roleId: 'role-CHAPTER_ADMIN', roleCode: 'CHAPTER_ADMIN', roleName: 'Chapter Admin' }, createdAt: '2026-02-01T09:30:00Z', updatedAt: '2026-02-01T09:30:00Z' },
];

// Kept separate from adminUsers (not a field on it) so no handler that returns adminUsers rows
// (e.g. GET /api/v1/users) can ever leak a password into a response, mirroring UserDto's own
// comment on why passwordHash never appears there either. Plain-text compare purely because this
// is a mock - the real backend only ever stores a BCrypt hash and compares via PasswordEncoder.
const mockCredentials: Record<string, string> = {
  'ramesh.agrawal@example.com': 'password123',
  '9876500001': 'password123',
  'chapter.admin@example.com': 'password123',
  '9876500002': 'password123',
};

let nextFamilyId = mockFamilies.length + 1;
let nextMemberId =
  Object.values(mockFamilyMembers).reduce((max, members) => Math.max(max, members.length), 0) + 1;

// Mutable copies so registrations/added members persist for the life of the page session.
const families: Family[] = [...mockFamilies];
const familyMembers: Record<string, FamilyMember[]> = Object.fromEntries(
  Object.entries(mockFamilyMembers).map(([familyId, members]) => [familyId, [...members]]),
);

let nextEventId = mockEvents.length + 1;
let nextRegistrationId = 1;
const events: EventItem[] = [...mockEvents];
const eventRegistrations: EventRegistration[] = [];

function computeAge(dateOfBirth: string): number {
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return 0;
  const now = new Date();
  let age = now.getFullYear() - dob.getFullYear();
  const monthDiff = now.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < dob.getDate())) age -= 1;
  return age;
}

// Mirrors EventServiceImpl.resolveEventsInScope - viewChapter tier and the no-tier floor collapse
// to the same "own chapter" filter here too (every mock chapter shares one mock state, same
// simplification the /families handler above already makes for its own state tier).
function resolveEventsInScope(caller: DecodedMockToken, permissions: string[]): EventItem[] {
  if (permissions.includes('VIEW_ALL_EVENTS')) return events;
  return events.filter((e) => e.chapterId === caller.chapterId);
}

function matchesEventFilters(
  event: EventItem,
  filters: { search?: string | null; category?: string | null; timeframe?: string | null; status?: string | null },
): boolean {
  const search = filters.search?.toLowerCase();
  if (search && !(event.title.toLowerCase().includes(search) || (event.description ?? '').toLowerCase().includes(search))) {
    return false;
  }
  if (filters.category && event.category?.toLowerCase() !== filters.category.toLowerCase()) return false;
  if (filters.timeframe) {
    const today = new Date().toISOString().slice(0, 10);
    if (filters.timeframe === 'UPCOMING' && event.eventDate < today) return false;
    if (filters.timeframe === 'PAST' && event.eventDate >= today) return false;
  }
  if (filters.status && event.status !== filters.status) return false;
  return true;
}

function applyEventStatus(eventId: string, status: EventStatus) {
  const event = events.find((e) => e.id === eventId);
  if (!event) return HttpResponse.json({ message: 'Event not found' }, { status: 404 });
  event.status = status;
  event.updatedAt = new Date().toISOString();
  return HttpResponse.json(event, { status: 200 });
}

// India FY (Apr-Mar), start-year form - mirrors backend/membership-service's FinancialYearUtil.
// Duplicated here rather than imported (mock handlers don't import page-level utilities) - kept in
// step by hand, the same way MOCK_ROLE_PERMISSIONS is kept in step with the real RBAC migration.
function currentFinancialYear(date: Date = new Date()): number {
  const month = date.getMonth() + 1;
  return month >= 4 ? date.getFullYear() : date.getFullYear() - 1;
}

let nextTransactionId = mockFamilies.length + 1;

// One seed transaction (family-1, last FY) so the mock demonstrates PENDING_RENEWAL out of the
// box; family-2 has none, so it starts EXPIRED. Mutable so RecordTransactionDialog's create/edit
// flows persist for the session, same pattern as `families` above.
const membershipTransactions: MembershipTransaction[] = [
  {
    id: 'txn-1',
    familyId: 'family-1',
    financialYear: currentFinancialYear() - 1,
    amount: 250,
    paymentDate: `${currentFinancialYear() - 1}-05-01`,
    paymentMode: 'CASH',
    transactionRef: 'TXN-SEED-1',
    remarks: null,
    createdBy: null,
    createdAt: `${currentFinancialYear() - 1}-05-01T00:00:00Z`,
    updatedBy: null,
    updatedAt: null,
  },
];

// Mirrors MembershipServiceImpl.computeStatus's grace-period rule exactly (paid targetFy ->
// ACTIVE; paid only the immediately preceding FY -> PENDING_RENEWAL; otherwise EXPIRED) - same
// "kept in step by hand" duplication as MOCK_ROLE_PERMISSIONS's relationship to the real RBAC
// migration, flagged here for the same reason.
function computeMockStatus(familyId: string, targetFy: number): MembershipStatusSummary {
  const rows = membershipTransactions.filter((t) => t.familyId === familyId);
  const targetRow = rows.find((t) => t.financialYear === targetFy);
  const paidYearsUpToTarget = rows.filter((t) => t.financialYear <= targetFy).map((t) => t.financialYear);
  const lastPaidYear = paidYearsUpToTarget.length > 0 ? Math.max(...paidYearsUpToTarget) : null;
  const lastPaidRow = lastPaidYear !== null ? rows.find((t) => t.financialYear === lastPaidYear) ?? null : null;

  let status: MembershipStatus;
  if (targetRow) {
    status = 'ACTIVE';
  } else if (lastPaidYear !== null && lastPaidYear === targetFy - 1) {
    status = 'PENDING_RENEWAL';
  } else {
    status = 'EXPIRED';
  }

  return {
    familyId,
    status,
    currentFinancialYear: targetFy,
    currentFinancialYearPaid: Boolean(targetRow),
    lastPaymentDate: lastPaidRow?.paymentDate ?? null,
    lastPaidFinancialYear: lastPaidYear,
  };
}

// A structurally valid JWT (base64url, matching fixtures.ts's buildMockJwt) for the account that
// was actually verified - mirrors the real local-auth/token endpoint's claim shape
// (LocalTokenController), issued only after DbLocalCredentialAuthenticator-equivalent checks below.
function buildToken(account: (typeof adminUsers)[number]): string {
  const base64Url = (value: string) =>
    btoa(value).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  const header = base64Url(JSON.stringify({ alg: 'none', typ: 'JWT' }));
  const now = Math.floor(Date.now() / 1000);
  const payload = base64Url(
    JSON.stringify({
      sub: account.id,
      email: account.email,
      chapter_id: account.chapterId,
      roles: [account.role.roleCode],
      role_id: account.role.roleId,
      role_name: account.role.roleName,
      iat: now,
      exp: now + 7200,
    }),
  );
  return `${header}.${payload}.mock-signature`;
}

export const handlers = [
  // Auth - mirrors DbLocalCredentialAuthenticator's three rejection reasons and their exact
  // wording (see LoginPage's error handling, which shows this message as-is).
  http.post('/api/v1/local-auth/token', async ({ request }) => {
    const body = (await request.json()) as { identifier?: string; password?: string };
    const identifier = body.identifier?.trim() ?? '';
    const password = body.password ?? '';

    const account = identifier.includes('@')
      ? adminUsers.find((user) => user.email.toLowerCase() === identifier.toLowerCase())
      : adminUsers.find((user) => user.mobileNumber === identifier);

    if (!account) {
      return HttpResponse.json(
        { message: 'User account does not exist. Please register first.' },
        { status: 401 },
      );
    }
    if (account.status !== 'ACTIVE') {
      return HttpResponse.json(
        { message: 'Your account is inactive. Please contact administrator.' },
        { status: 401 },
      );
    }
    const expectedPassword = mockCredentials[identifier.includes('@') ? account.email : account.mobileNumber];
    if (expectedPassword === undefined || expectedPassword !== password) {
      return HttpResponse.json({ message: 'Invalid username or password.' }, { status: 401 });
    }

    return HttpResponse.json(
      {
        accessToken: buildToken(account),
        tokenType: 'Bearer',
        expiresInSeconds: 7200,
      },
      { status: 200 },
    );
  }),

  // The signed-in user's role, menus and permissions. Mirrors user-service's /me, including the
  // fact that permissions are derived from the role rather than sent by the client - the mock
  // reads the role out of the caller's own token so switching roles at the login screen changes
  // what the shell renders, exactly as it does against the real backend.
  http.get('/api/v1/me', ({ request }) => {
    const roleCode = roleFromAuthHeader(request.headers.get('Authorization'));
    const permissions = MOCK_ROLE_PERMISSIONS[roleCode] ?? MOCK_ROLE_PERMISSIONS.USER ?? [];
    const menuKeys = MOCK_ROLE_MENUS[roleCode] ?? MOCK_ROLE_MENUS.USER ?? [];

    return HttpResponse.json(
      {
        userId: MOCK_USER_ID,
        email: 'demo@example.com',
        chapterId: mockBranches[0]?.id ?? 'branch-indore',
        role: { roleId: `role-${roleCode}`, roleCode, roleName: roleCode },
        menus: MOCK_MENUS.filter((menu) => menuKeys.includes(menu.menuKey)),
        permissions,
      },
      { status: 200 },
    );
  }),

  // ---- RBAC administration (user-service's RbacController)

  http.get('/api/v1/roles', () => HttpResponse.json(adminRoles, { status: 200 })),

  http.post('/api/v1/roles', async ({ request }) => {
    const body = (await request.json()) as { roleCode: string; roleName: string; description?: string };
    if (adminRoles.some((role) => role.roleCode === body.roleCode)) {
      return HttpResponse.json({ message: `Role already exists: ${body.roleCode}` }, { status: 400 });
    }
    const created = {
      roleId: `role-${body.roleCode}`,
      roleCode: body.roleCode,
      roleName: body.roleName,
      description: body.description ?? null,
      active: true,
      permissionCodes: [] as string[],
      menuKeys: [] as string[],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    adminRoles.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.put('/api/v1/roles/:roleId', async ({ params, request }) => {
    const body = (await request.json()) as { roleName: string; description?: string };
    const role = adminRoles.find((r) => r.roleId === params.roleId);
    if (!role) return HttpResponse.json({ message: 'Role not found' }, { status: 404 });
    role.roleName = body.roleName;
    role.description = body.description ?? null;
    return HttpResponse.json(role, { status: 200 });
  }),

  http.put('/api/v1/roles/:roleId/active', async ({ params, request }) => {
    const body = (await request.json()) as { active: boolean };
    const role = adminRoles.find((r) => r.roleId === params.roleId);
    if (!role) return HttpResponse.json({ message: 'Role not found' }, { status: 404 });
    role.active = body.active;
    return HttpResponse.json(role, { status: 200 });
  }),

  http.put('/api/v1/roles/:roleId/permissions', async ({ params, request }) => {
    const body = (await request.json()) as { permissionCodes: string[] };
    const role = adminRoles.find((r) => r.roleId === params.roleId);
    if (!role) return HttpResponse.json({ message: 'Role not found' }, { status: 404 });
    role.permissionCodes = body.permissionCodes;
    return HttpResponse.json(role, { status: 200 });
  }),

  http.put('/api/v1/roles/:roleId/menus', async ({ params, request }) => {
    const body = (await request.json()) as { menuKeys: string[] };
    const role = adminRoles.find((r) => r.roleId === params.roleId);
    if (!role) return HttpResponse.json({ message: 'Role not found' }, { status: 404 });
    role.menuKeys = body.menuKeys;
    return HttpResponse.json(role, { status: 200 });
  }),

  http.get('/api/v1/permissions', () =>
    HttpResponse.json(
      ADMIN_PERMISSIONS.map((code) => ({
        permissionId: `perm-${code}`,
        permissionCode: code,
        permissionName: code
          .toLowerCase()
          .split('_')
          .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
          .join(' '),
        description: null,
      })),
      { status: 200 },
    ),
  ),

  http.get('/api/v1/menus', () => HttpResponse.json(MOCK_MENUS, { status: 200 })),

  http.get('/api/v1/users', () => HttpResponse.json(adminUsers, { status: 200 })),

  http.put('/api/v1/users/:userId/role', async ({ params, request }) => {
    const body = (await request.json()) as { roleCode: string };
    const user = adminUsers.find((u) => u.id === params.userId);
    if (!user) return HttpResponse.json({ message: 'User not found' }, { status: 404 });
    const role = adminRoles.find((r) => r.roleCode === body.roleCode);
    if (!role) return HttpResponse.json({ message: `Unknown role code: ${body.roleCode}` }, { status: 400 });
    user.role = { roleId: role.roleId, roleCode: role.roleCode, roleName: role.roleName };
    return HttpResponse.json(user, { status: 200 });
  }),

  // Chapters / Branches (branchesApi calls /chapters - "Branch" is just the display-layer name).
  // ChapterDto carries createdAt, which BranchSummary (the shared fixture shape) does not, so it's
  // added here rather than widening the fixture that every other consumer uses.
  http.get('/api/v1/chapters', () =>
    HttpResponse.json(
      chapters.map((chapter) => ({ ...chapter, createdAt: chapter.createdAt ?? '2026-01-01T00:00:00Z' })),
      { status: 200 },
    ),
  ),

  http.post('/api/v1/chapters', async ({ request }) => {
    const body = (await request.json()) as { name: string; city: string; state: string };
    const created = { id: `branch-${chapters.length + 1}`, ...body, createdAt: new Date().toISOString() };
    chapters.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.put('/api/v1/chapters/:chapterId', async ({ params, request }) => {
    const body = (await request.json()) as { name: string; city: string; state: string };
    const chapter = chapters.find((c) => c.id === params.chapterId);
    if (!chapter) return HttpResponse.json({ message: 'Chapter not found' }, { status: 404 });
    Object.assign(chapter, body);
    return HttpResponse.json(chapter, { status: 200 });
  }),

  // Families - scope-aware, mirroring FamilyServiceImpl.resolveFamiliesInScope: broadest
  // permission wins (VIEW_ALL_FAMILIES > VIEW_STATE_FAMILIES/VIEW_CHAPTER_FAMILIES - collapsed to
  // one tier here since every mock family shares the same single mock chapter > owner-only).
  // FamiliesListPage.tsx no longer does any of this narrowing itself (see permissions.ts's
  // visibleFamilies comment) - the real backend is authoritative, so the mock has to be too.
  http.get('/api/v1/families', ({ request }) => {
    const caller = decodeMockToken(request.headers.get('Authorization'));
    const permissions = MOCK_ROLE_PERMISSIONS[caller.role] ?? MOCK_ROLE_PERMISSIONS.USER ?? [];
    let scoped: Family[];
    if (permissions.includes('VIEW_ALL_FAMILIES')) {
      scoped = families;
    } else if (permissions.includes('VIEW_STATE_FAMILIES') || permissions.includes('VIEW_CHAPTER_FAMILIES')) {
      scoped = families.filter((f) => f.chapterId === caller.chapterId);
    } else {
      // Same ownership rule the app itself uses (isOwnFamily: ownerUserId when recorded, email
      // fallback for legacy rows with none) - reused rather than reimplemented so the mock can
      // never drift from what "own family" actually means.
      scoped = families.filter((f) => isOwnFamily(f, { id: caller.sub, email: caller.email }));
    }

    // Optional search params, same three FamilyController#listFamilies added for
    // RecordTransactionDialog's family search - each is independently optional and ANDed
    // together (a blank/absent one imposes no filter), matching family-service's own semantics
    // (FamilyServiceImpl#applySearchFilters) exactly - NOT an OR/"any field matches" search.
    const url = new URL(request.url);
    const headOfFamilyName = (url.searchParams.get('headOfFamilyName') || '').toLowerCase();
    const mobileNumber = url.searchParams.get('mobileNumber') || '';
    const areaLocality = (url.searchParams.get('areaLocality') || '').toLowerCase();
    scoped = scoped
      .filter((f) => !headOfFamilyName || f.headOfFamilyName.toLowerCase().includes(headOfFamilyName))
      .filter((f) => !mobileNumber || (f.mobileNumber || '').includes(mobileNumber))
      .filter((f) => !areaLocality || (f.areaLocality || '').toLowerCase().includes(areaLocality));

    return HttpResponse.json(scoped, { status: 200 });
  }),

  http.get('/api/v1/families/:familyId', ({ params }) => {
    const family = families.find((f) => f.id === params.familyId);
    if (!family) return HttpResponse.json({ message: 'Family not found' }, { status: 404 });
    return HttpResponse.json(family, { status: 200 });
  }),

  http.post('/api/v1/families', async ({ request }) => {
    const body = (await request.json()) as CreateFamilyRequest;
    const id = `family-${nextFamilyId++}`;
    const branch = mockBranches[0] ?? null;
    const headOfFamilyName = [body.headFirstName, body.headMiddleName, body.headLastName]
      .filter(Boolean)
      .join(' ');

    const newFamily: Family = {
      id,
      familyCode: `FAM-${id.toUpperCase()}`,
      chapterId: branch?.id ?? '',
      branch,
      headFirstName: body.headFirstName,
      headMiddleName: body.headMiddleName || null,
      headLastName: body.headLastName,
      headOfFamilyName,
      familyName: body.familyName,
      headGender: body.headGender,
      headDateOfBirth: body.headDateOfBirth,
      mobileNumber: body.mobileNumber,
      email: body.email,
      aadhaarNumber: body.aadhaarNumber || null,
      address: body.address,
      district: body.district,
      country: body.country,
      state: body.state,
      city: body.district,
      areaLocality: body.areaLocality,
      pinCode: body.pinCode,
      samaj: body.samaj,
      gotra: body.gotra,
      nativePlace: body.nativePlace,
      occupationBusinessType: body.occupationBusinessType,
      annualIncomeRange: body.annualIncomeRange,
      familyCategory: body.familyCategory,
      ownTwoWheeler: body.ownTwoWheeler,
      ownFourWheeler: body.ownFourWheeler,
      ownHome: body.ownHome,
      ownPlot: body.ownPlot,
      willingToContribute: body.willingToContribute,
      hasProfilePhoto: false,
      ownerUserId: MOCK_USER_ID,
      createdAt: new Date().toISOString(),
    };

    families.push(newFamily);
    familyMembers[id] = [];
    return HttpResponse.json(newFamily, { status: 201 });
  }),

  // Family Members
  http.get('/api/v1/families/:familyId/members', ({ params }) => {
    const familyId = params.familyId as string;
    return HttpResponse.json(familyMembers[familyId] ?? [], { status: 200 });
  }),

  http.post('/api/v1/families/:familyId/members', async ({ params, request }) => {
    const familyId = params.familyId as string;
    const body = (await request.json()) as CreateFamilyMemberRequest;

    const newMember: FamilyMember = {
      id: `member-${nextMemberId++}`,
      familyId,
      name: body.name,
      relationshipToHead: body.relationshipToHead,
      dateOfBirth: body.dateOfBirth,
      age: computeAge(body.dateOfBirth),
      gender: body.gender,
      maritalStatus: body.maritalStatus ?? 'SINGLE',
      education: body.education || null,
      instituteName: body.instituteName || null,
      profession: body.profession || null,
      companyName: body.companyName || null,
      mobileNumber: body.mobileNumber || null,
      email: body.email || null,
      bloodGroup: body.bloodGroup,
    };

    if (!familyMembers[familyId]) familyMembers[familyId] = [];
    familyMembers[familyId].push(newMember);
    return HttpResponse.json(newMember, { status: 201 });
  }),

  http.get('/api/v1/families/:familyId/photo', () =>
    HttpResponse.json({ message: 'No photo uploaded' }, { status: 404 }),
  ),
  http.post('/api/v1/families/:familyId/photo', () => new HttpResponse(null, { status: 204 })),

  // Membership - see MembershipController's real endpoint table; own-family/transaction-history
  // reads don't narrow by caller here (families are already the full mock set), which is fine for
  // dev:mock/component tests since the real isolation guarantee is backend-owned and already
  // covered by MembershipControllerTest/MembershipServiceImplTest - this mock only needs to look
  // plausible, not re-prove the RBAC contract.
  http.get('/api/v1/memberships/family/:familyId/status', ({ params }) => {
    const familyId = params.familyId as string;
    if (!families.some((f) => f.id === familyId)) {
      return HttpResponse.json({ message: 'Family not found' }, { status: 404 });
    }
    return HttpResponse.json(computeMockStatus(familyId, currentFinancialYear()), { status: 200 });
  }),

  http.get('/api/v1/memberships/family/:familyId/transactions', ({ params }) => {
    const familyId = params.familyId as string;
    const rows = membershipTransactions
      .filter((t) => t.familyId === familyId)
      .sort((a, b) => b.paymentDate.localeCompare(a.paymentDate));
    return HttpResponse.json(rows, { status: 200 });
  }),

  http.post('/api/v1/memberships/transactions', async ({ request }) => {
    const body = (await request.json()) as RecordTransactionRequest;
    const duplicate = membershipTransactions.some(
      (t) => t.familyId === body.familyId && t.financialYear === body.financialYear,
    );
    if (duplicate) {
      return HttpResponse.json(
        { message: 'A transaction already exists for this family in this financial year' },
        { status: 400 },
      );
    }
    const now = new Date().toISOString();
    const created: MembershipTransaction = {
      id: `txn-${nextTransactionId++}`,
      familyId: body.familyId,
      financialYear: body.financialYear,
      amount: body.amount,
      paymentDate: body.paymentDate,
      paymentMode: body.paymentMode,
      transactionRef: body.transactionRef || null,
      remarks: body.remarks || null,
      createdBy: MOCK_USER_ID,
      createdAt: now,
      updatedBy: null,
      updatedAt: null,
    };
    membershipTransactions.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.put('/api/v1/memberships/transactions/:transactionId', async ({ params, request }) => {
    const body = (await request.json()) as UpdateTransactionRequest;
    const transaction = membershipTransactions.find((t) => t.id === params.transactionId);
    if (!transaction) return HttpResponse.json({ message: 'Transaction not found' }, { status: 404 });
    transaction.amount = body.amount;
    transaction.paymentDate = body.paymentDate;
    transaction.paymentMode = body.paymentMode;
    transaction.transactionRef = body.transactionRef || null;
    transaction.remarks = body.remarks || null;
    transaction.updatedBy = MOCK_USER_ID;
    transaction.updatedAt = new Date().toISOString();
    return HttpResponse.json(transaction, { status: 200 });
  }),

  http.get('/api/v1/memberships/members', ({ request }) => {
    const url = new URL(request.url);
    const financialYear = Number(url.searchParams.get('financialYear')) || currentFinancialYear();
    const statusFilter = url.searchParams.get('status') as MembershipStatus | null;
    const rows = families
      .map((f) => computeMockStatus(f.id, financialYear))
      .filter((row) => !statusFilter || row.status === statusFilter);
    return HttpResponse.json(rows, { status: 200 });
  }),

  http.get('/api/v1/memberships/reports/pending', ({ request }) => {
    const url = new URL(request.url);
    const financialYear = Number(url.searchParams.get('financialYear')) || currentFinancialYear();
    const familyIdQuery = (url.searchParams.get('familyId') || '').toLowerCase();
    const headOfFamilyNameQuery = (url.searchParams.get('headOfFamilyName') || '').toLowerCase();
    const mobileNumberQuery = url.searchParams.get('mobileNumber') || '';
    const areaLocalityQuery = (url.searchParams.get('areaLocality') || '').toLowerCase();

    const rows = families
      .filter((f) => !headOfFamilyNameQuery || f.headOfFamilyName.toLowerCase().includes(headOfFamilyNameQuery))
      .filter((f) => !mobileNumberQuery || (f.mobileNumber || '').includes(mobileNumberQuery))
      .filter((f) => !areaLocalityQuery || (f.areaLocality || '').toLowerCase().includes(areaLocalityQuery))
      .filter((f) => !familyIdQuery || f.familyCode.toLowerCase().includes(familyIdQuery))
      .map((f) => {
        const status = computeMockStatus(f.id, financialYear);
        return {
          familyId: f.id,
          familyCode: f.familyCode,
          headOfFamilyName: f.headOfFamilyName,
          mobileNumber: f.mobileNumber || null,
          areaLocality: f.areaLocality || null,
          chapterId: f.chapterId,
          chapterName: f.branch?.name ?? null,
          status: status.status,
          lastPaidFinancialYear: status.lastPaidFinancialYear,
          lastPaymentDate: status.lastPaymentDate,
        };
      })
      .filter((row) => row.status !== 'ACTIVE');

    return HttpResponse.json(rows, { status: 200 });
  }),

  http.get('/api/v1/memberships/reports/collection-summary', ({ request }) => {
    const url = new URL(request.url);
    const financialYear = Number(url.searchParams.get('financialYear')) || currentFinancialYear();
    const chapter = mockBranches[0];

    let familiesActive = 0;
    let familiesPending = 0;
    let familiesExpired = 0;
    for (const family of families) {
      const status = computeMockStatus(family.id, financialYear).status;
      if (status === 'ACTIVE') familiesActive += 1;
      else if (status === 'PENDING_RENEWAL') familiesPending += 1;
      else familiesExpired += 1;
    }
    const totalCollected = membershipTransactions
      .filter((t) => t.financialYear === financialYear)
      .reduce((sum, t) => sum + t.amount, 0);

    return HttpResponse.json(
      {
        chapterId: chapter?.id ?? '',
        chapterName: chapter?.name ?? null,
        financialYear,
        totalCollected,
        familiesActive,
        familiesPending,
        familiesExpired,
      },
      { status: 200 },
    );
  }),

  // Matrimony
  http.get('/api/v1/matrimony/consent/me', () =>
    HttpResponse.json(
      {
        individualId: 'member-1',
        fullName: 'Ramesh Agrawal',
        consentGiven: false,
        consentScope: null,
        consentGivenBy: null,
        consentGivenAt: null,
      },
      { status: 200 },
    ),
  ),
  http.put('/api/v1/matrimony/consent/me', async ({ request }) => {
    const body = (await request.json()) as { consentGiven: boolean; consentScope: string | null };
    return HttpResponse.json(
      {
        individualId: 'member-1',
        fullName: 'Ramesh Agrawal',
        consentGiven: body.consentGiven,
        consentScope: body.consentScope,
        consentGivenBy: body.consentGiven ? 'member-1' : null,
        consentGivenAt: body.consentGiven ? new Date().toISOString() : null,
      },
      { status: 200 },
    );
  }),
  http.post('/api/v1/matrimony/consent/me/erasure-request', () => new HttpResponse(null, { status: 204 })),
  http.get('/api/v1/matrimony/directory', () => HttpResponse.json([], { status: 200 })),

  // Events - see EventController's real endpoint table; member-facing browse always forces
  // status=PUBLISHED regardless of what a caller passes, matching EventServiceImpl.listPublishedEvents.
  http.get('/api/v1/events', ({ request }) => {
    const caller = decodeMockToken(request.headers.get('Authorization'));
    const permissions = MOCK_ROLE_PERMISSIONS[caller.role] ?? MOCK_ROLE_PERMISSIONS.USER ?? [];
    const url = new URL(request.url);
    const rows = resolveEventsInScope(caller, permissions)
      .filter((e) => e.status === 'PUBLISHED')
      .filter((e) =>
        matchesEventFilters(e, {
          search: url.searchParams.get('search'),
          category: url.searchParams.get('category'),
          timeframe: url.searchParams.get('timeframe'),
        }),
      );
    return HttpResponse.json(rows, { status: 200 });
  }),

  http.get('/api/v1/events/admin', ({ request }) => {
    const caller = decodeMockToken(request.headers.get('Authorization'));
    const permissions = MOCK_ROLE_PERMISSIONS[caller.role] ?? MOCK_ROLE_PERMISSIONS.USER ?? [];
    const url = new URL(request.url);
    const rows = resolveEventsInScope(caller, permissions).filter((e) =>
      matchesEventFilters(e, {
        search: url.searchParams.get('search'),
        category: url.searchParams.get('category'),
        timeframe: url.searchParams.get('timeframe'),
        status: url.searchParams.get('status'),
      }),
    );
    return HttpResponse.json(rows, { status: 200 });
  }),

  http.get('/api/v1/events/:eventId', ({ params, request }) => {
    const caller = decodeMockToken(request.headers.get('Authorization'));
    const permissions = MOCK_ROLE_PERMISSIONS[caller.role] ?? MOCK_ROLE_PERMISSIONS.USER ?? [];
    const event = events.find((e) => e.id === params.eventId);
    if (!event) return HttpResponse.json({ message: 'Event not found' }, { status: 404 });
    const canManage = permissions.includes('MANAGE_EVENTS');
    if (event.status !== 'PUBLISHED' && !canManage) {
      return HttpResponse.json({ message: 'Event not found' }, { status: 404 });
    }
    return HttpResponse.json(event, { status: 200 });
  }),

  http.post('/api/v1/events', async ({ request }) => {
    const caller = decodeMockToken(request.headers.get('Authorization'));
    const body = (await request.json()) as CreateEventRequest;
    const now = new Date().toISOString();
    const created: EventItem = {
      id: `event-${nextEventId++}`,
      chapterId: caller.chapterId,
      chapterName: mockBranches.find((b) => b.id === caller.chapterId)?.name ?? null,
      title: body.title,
      description: body.description || null,
      category: body.category || null,
      eventDate: body.eventDate,
      startTime: body.startTime,
      endTime: body.endTime,
      location: body.location || null,
      organizerName: body.organizerName || null,
      contactDetails: body.contactDetails || null,
      status: 'DRAFT',
      hasBanner: false,
      createdBy: caller.sub,
      createdAt: now,
      updatedBy: null,
      updatedAt: null,
    };
    events.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.put('/api/v1/events/:eventId', async ({ params, request }) => {
    const caller = decodeMockToken(request.headers.get('Authorization'));
    const body = (await request.json()) as CreateEventRequest;
    const event = events.find((e) => e.id === params.eventId);
    if (!event) return HttpResponse.json({ message: 'Event not found' }, { status: 404 });
    event.title = body.title;
    event.description = body.description || null;
    event.category = body.category || null;
    event.eventDate = body.eventDate;
    event.startTime = body.startTime;
    event.endTime = body.endTime;
    event.location = body.location || null;
    event.organizerName = body.organizerName || null;
    event.contactDetails = body.contactDetails || null;
    event.updatedBy = caller.sub;
    event.updatedAt = new Date().toISOString();
    return HttpResponse.json(event, { status: 200 });
  }),

  http.delete('/api/v1/events/:eventId', ({ params }) => {
    const index = events.findIndex((e) => e.id === params.eventId);
    if (index === -1) return HttpResponse.json({ message: 'Event not found' }, { status: 404 });
    events.splice(index, 1);
    return new HttpResponse(null, { status: 204 });
  }),

  http.post('/api/v1/events/:eventId/publish', ({ params }) => applyEventStatus(params.eventId as string, 'PUBLISHED')),
  http.post('/api/v1/events/:eventId/unpublish', ({ params }) => applyEventStatus(params.eventId as string, 'DRAFT')),
  http.post('/api/v1/events/:eventId/cancel', ({ params }) => applyEventStatus(params.eventId as string, 'CANCELLED')),

  // familyId-based, matching RegisterFamilyRequest - see eventsApi.ts's comment on why this
  // replaced the old attendeeCount model (zero backend support, wrong endpoint/payload shape).
  http.post('/api/v1/events/:eventId/registrations', async ({ params, request }) => {
    const eventId = params.eventId as string;
    const event = events.find((e) => e.id === eventId);
    if (!event) return HttpResponse.json({ message: 'Event not found' }, { status: 404 });
    if (event.status !== 'PUBLISHED') {
      return HttpResponse.json({ message: 'Cannot register for an event that is not published' }, { status: 400 });
    }
    const body = (await request.json()) as RegisterEventRequest;
    if (eventRegistrations.some((r) => r.eventId === eventId && r.familyId === body.familyId)) {
      return HttpResponse.json({ message: 'Family already registered for this event' }, { status: 400 });
    }
    const created: EventRegistration = {
      id: `registration-${nextRegistrationId++}`,
      eventId,
      familyId: body.familyId,
      registeredAt: new Date().toISOString(),
    };
    eventRegistrations.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.get('/api/v1/events/:eventId/registrations', ({ params }) => {
    const rows = eventRegistrations.filter((r) => r.eventId === params.eventId);
    return HttpResponse.json(rows, { status: 200 });
  }),

  http.post('/api/v1/events/:eventId/banner', ({ params }) => {
    const event = events.find((e) => e.id === params.eventId);
    if (event) event.hasBanner = true;
    return new HttpResponse(null, { status: 204 });
  }),

  http.get('/api/v1/events/:eventId/banner', () =>
    HttpResponse.json({ message: 'No banner uploaded' }, { status: 404 }),
  ),

  // Analytics
  http.get('/api/v1/analytics/summary', () =>
    HttpResponse.json(
      {
        totalFamilies: families.length,
        totalPopulation: 0,
        activeMembers: 0,
        membershipCollection: 0,
        eligibleBoys: 0,
        eligibleGirls: 0,
        familiesByChapter: [],
        membershipStatusSplit: [],
        ageEligibilityDistribution: [],
      },
      { status: 200 },
    ),
  ),
];

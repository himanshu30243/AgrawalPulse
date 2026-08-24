import type { ReactElement, ReactNode } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '@/auth/AuthContext';
import { tokenStorage } from '@/auth/tokenStorage';
import type { Role } from '@/types/domain';

export interface TestAuthUser {
  /**
   * The token's sub claim, i.e. AuthUser.id. Defaults to 'test-user' when omitted. Only needs
   * overriding when a test's scenario depends on ownerUserId matching (or deliberately NOT
   * matching) a specific family/record - most tests don't care and can rely on the default.
   */
  id?: string;
  email?: string;
  chapterId?: string;
  /**
   * Role codes stamped into the token. The MSW /me handler derives menus and permissions from the
   * first one, exactly as user-service does - so a test says "render as a CHAPTER_ADMIN" and gets
   * that role's real permission set rather than hand-listing codes.
   */
  roles: Role[];
}

/**
 * Seeds a signed-in user by writing a structurally valid (fake-signed) token to tokenStorage,
 * which is exactly where AuthProvider reads from on mount. Needed now that pages branch on the
 * user's role - a bare render has no user at all, so anything role-gated renders empty and the
 * test tells you nothing about the case you meant to cover.
 */
function seedAuthUser({ id, email, chapterId, roles }: TestAuthUser): void {
  const base64Url = (value: string) =>
    btoa(value).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: 'none', typ: 'JWT' }));
  const payload = base64Url(
    JSON.stringify({
      sub: id ?? 'test-user',
      email: email ?? 'test-user@example.com',
      chapter_id: chapterId ?? 'branch-indore',
      roles,
      iat: now,
      exp: now + 3600,
    }),
  );
  tokenStorage.set(`${header}.${payload}.test-signature`);
}

interface RenderWithProvidersOptions {
  // Starting URL (path + optional query string) - defaults to '/'. Needed for components that
  // read useSearchParams (e.g. MemberRegistrationPage's ?familyId=).
  route?: string;
  // React Router's `location.state` for the initial entry - needed for components that read
  // useLocation().state instead of/alongside query params.
  state?: unknown;
  // Extra probe routes rendered alongside `ui` inside a <Routes> tree, keyed by path - lets a
  // test assert that a navigate() call actually landed on the expected path (e.g. clicking
  // "Register New Family" navigates to /families/register) without rendering that whole page,
  // just a marker element. No existing test in this codebase needed this before
  // FamiliesListPage's registration flow moved from a Dialog to real route navigation.
  extraRoutes?: Record<string, ReactNode>;
  // Signs a user in before mounting, so role-gated UI renders as that user. Omit to render
  // signed-out.
  authUser?: TestAuthUser;
}

// Every page component assumes it's inside a Router (useLocation/useNavigate) and an
// AuthProvider (useAuth) - this wraps both so tests read like "render the real app shell",
// same as what actually mounts these components in src/App.tsx.
function AllProviders({
  children,
  route,
  state,
  extraRoutes,
}: { children: ReactNode } & RenderWithProvidersOptions) {
  // `route` may include a query string (e.g. '/member-registration?familyId=1') - MemoryRouter's
  // initialEntries needs pathname/search split apart, or useSearchParams reads an empty query
  // string (the whole thing lands in `pathname` instead, which isn't parsed for query params).
  const url = new URL(route ?? '/', 'http://localhost');
  return (
    <MemoryRouter initialEntries={[{ pathname: url.pathname, search: url.search, state }]}>
      <AuthProvider>
        {extraRoutes ? (
          <Routes>
            <Route path="*" element={children} />
            {Object.entries(extraRoutes).map(([path, element]) => (
              <Route key={path} path={path} element={element} />
            ))}
          </Routes>
        ) : (
          children
        )}
      </AuthProvider>
    </MemoryRouter>
  );
}

export function renderWithProviders(ui: ReactElement, options?: RenderWithProvidersOptions) {
  // Must happen before render: AuthProvider reads tokenStorage in a mount effect.
  if (options?.authUser) seedAuthUser(options.authUser);
  return render(ui, { wrapper: (props) => <AllProviders {...props} {...options} /> });
}

export * from '@testing-library/react';

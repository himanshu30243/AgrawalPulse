import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { NavDrawer } from './NavDrawer';

// The navigation is now entirely server-driven (GET /api/v1/me), so these assert against what the
// mock /me returns for each role - which mirrors user-service's V2 seed data.
function renderNav(roles: string[]) {
  return renderWithProviders(
    <NavDrawer variant="permanent" open onClose={() => undefined} />,
    { authUser: { roles } },
  );
}

describe('NavDrawer', () => {
  it('renders only the menus a plain USER may see', async () => {
    renderNav(['USER']);

    await waitFor(() => expect(screen.getByText('Dashboard')).toBeInTheDocument());
    // "Families" not "Family": the translated nav.families label wins over the server's
    // menu_name, which is what keeps the shell bilingual.
    expect(screen.getByText('Families')).toBeInTheDocument();
    expect(screen.getByText('Membership')).toBeInTheDocument();
    expect(screen.getByText('Events')).toBeInTheDocument();

    // Per the spec, a USER must not see any of these.
    expect(screen.queryByText('Matrimony')).not.toBeInTheDocument();
    expect(screen.queryByText('Reports')).not.toBeInTheDocument();
    expect(screen.queryByText('User Management')).not.toBeInTheDocument();
    expect(screen.queryByText('Branch Management')).not.toBeInTheDocument();
    expect(screen.queryByText('Role Management')).not.toBeInTheDocument();
  });

  it('widens the menu for a CHAPTER_ADMIN without any frontend role check', async () => {
    renderNav(['CHAPTER_ADMIN']);

    await waitFor(() => expect(screen.getByText('Matrimony')).toBeInTheDocument());
    expect(screen.getByText('Reports')).toBeInTheDocument();
    expect(screen.getByText('Branch Management')).toBeInTheDocument();
    // Chapter admins manage their own chapter, not users or roles.
    expect(screen.queryByText('User Management')).not.toBeInTheDocument();
    expect(screen.queryByText('Role Management')).not.toBeInTheDocument();
  });

  it('gives ADMIN everything, including role management', async () => {
    renderNav(['ADMIN']);

    await waitFor(() => expect(screen.getByText('Role Management')).toBeInTheDocument());
    expect(screen.getByText('User Management')).toBeInTheDocument();
    expect(screen.getByText('Settings')).toBeInTheDocument();
  });

  it('renders a menu the frontend has never heard of', async () => {
    // The whole point of DB-driven menus: an administrator adds a row and it appears, with no
    // frontend release. An unmapped icon degrades to a placeholder rather than crashing.
    const { server } = await import('@/mocks/server');
    const { http, HttpResponse } = await import('msw');
    server.use(
      http.get('*/api/v1/me', () =>
        HttpResponse.json({
          userId: 'u1',
          email: 'demo@example.com',
          chapterId: 'branch-indore',
          role: { roleId: 'r1', roleCode: 'USER', roleName: 'User' },
          menus: [
            {
              menuId: 'm-new',
              menuKey: 'grievances',
              menuName: 'Grievance Desk',
              menuPath: '/grievances',
              icon: 'SomeIconWeDoNotShip',
              displayOrder: 5,
              parentMenuKey: null,
              active: true,
            },
          ],
          permissions: ['VIEW_DASHBOARD'],
        }),
      ),
    );

    renderNav(['USER']);

    await waitFor(() => expect(screen.getByText('Grievance Desk')).toBeInTheDocument());
  });
});

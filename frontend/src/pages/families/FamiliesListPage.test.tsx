import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor, within } from '@/test/test-utils';
import { mockFamilies, mockFamilyMembers } from '@/mocks/fixtures';
import FamiliesListPage from './FamiliesListPage';

// The page is role-scoped now (see auth/permissions.ts), so every render has to say who is
// looking - a signed-out render legitimately shows nothing at all.
describe('FamiliesListPage', () => {
  it('lists families from the (mock) backend showing the readable familyCode and branch, not the raw UUID', async () => {
    renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['ADMIN'] } });

    const family = mockFamilies[0];
    if (!family) throw new Error('fixture data missing');

    await waitFor(() => {
      expect(screen.getByText(family.familyCode)).toBeInTheDocument();
    });
    expect(screen.getByText(family.headOfFamilyName)).toBeInTheDocument();
    // Both fixture families share the same branch, so more than one row shows it - the point is
    // that it's shown at all (not the raw UUID), not that it's unique on the page.
    expect(screen.getAllByText(new RegExp(family.branch?.name ?? '', 'i')).length).toBeGreaterThan(0);
    // The raw UUID should not be what's shown for a family that does have a code.
    expect(screen.queryByText(family.id)).not.toBeInTheDocument();
  });

  it('opens the members dialog and shows the family-tree data for that family', async () => {
    renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['ADMIN'] } });
    const user = userEvent.setup();
    const family = mockFamilies[0];
    if (!family) throw new Error('fixture data missing');
    const members = mockFamilyMembers[family.id] ?? [];

    await waitFor(() => expect(screen.getByText(family.familyCode)).toBeInTheDocument());

    const viewButtons = screen.getAllByRole('button', { name: /view members/i });
    await user.click(viewButtons[0] as HTMLElement);

    // Scoped to the dialog specifically: the head of family's name (e.g. "Ramesh Agrawal")
    // legitimately appears twice on the page once the dialog opens - once in the underlying
    // families table, once again as the SELF row inside the members dialog.
    const dialog = await screen.findByRole('dialog');
    for (const member of members) {
      await waitFor(() => expect(within(dialog).getByText(member.name)).toBeInTheDocument());
    }
  });

  it('shows the Edit button for a role holding EDIT_FAMILY, and opens the edit dialog for that family', async () => {
    renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['ADMIN'] } });
    const user = userEvent.setup();
    const family = mockFamilies[0];
    if (!family) throw new Error('fixture data missing');

    await waitFor(() => expect(screen.getByText(family.familyCode)).toBeInTheDocument());

    const editButtons = screen.getAllByRole('button', { name: /^edit$/i });
    await user.click(editButtons[0] as HTMLElement);

    expect(await screen.findByText(/edit family/i)).toBeInTheDocument();
    expect(screen.getByLabelText('First Name')).toHaveValue(family.headFirstName);
  });

  it('hides the Edit button for a read-only role without EDIT_FAMILY', async () => {
    // Mirrors the "never offers registration to a role without the create permission" test above -
    // an auditor-like role that can view everything but edit nothing.
    const { server } = await import('@/mocks/server');
    const { http, HttpResponse } = await import('msw');
    server.use(
      http.get('*/api/v1/me', () =>
        HttpResponse.json({
          userId: 'read-only-user',
          email: 'auditor@example.com',
          chapterId: 'branch-indore',
          role: { roleId: 'role-auditor', roleCode: 'AUDITOR', roleName: 'Auditor' },
          menus: [],
          permissions: ['VIEW_FAMILY', 'VIEW_ALL_FAMILIES'],
        }),
      ),
    );

    renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['AUDITOR'] } });

    await waitFor(() => expect(screen.getByText('Ramesh Agrawal')).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: /^edit$/i })).not.toBeInTheDocument();
  });

  it('shows an empty state when there are no families', async () => {
    // Overrides the default mock handler for just this test to exercise the empty-list path,
    // which the "happy path" tests above never touch.
    const { server } = await import('@/mocks/server');
    const { http, HttpResponse } = await import('msw');
    server.use(http.get('*/api/v1/families', () => HttpResponse.json([])));

    renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['ADMIN'] } });

    await waitFor(() => {
      expect(screen.getByText(/no data/i)).toBeInTheDocument();
    });
  });

  it('navigates to the registration wizard route instead of opening a dialog', async () => {
    // Registration moved from a Dialog+form to a real route (FamilyRegistrationWizardPage) so
    // the multi-step wizard has its own URL/browser history entry - this asserts the button
    // actually navigates there, using a probe route standing in for the real (much heavier) page.
    renderWithProviders(<FamiliesListPage />, {
      authUser: { roles: ['ADMIN'] },
      extraRoutes: { '/families/register': <div>REGISTRATION_WIZARD_ROUTE</div> },
    });
    const user = userEvent.setup();

    // findBy, not getBy: the button only renders once GET /me has told us this user may create.
    await user.click(await screen.findByRole('button', { name: /register new family/i }));

    expect(await screen.findByText('REGISTRATION_WIZARD_ROUTE')).toBeInTheDocument();
  });

  it('filters the list by the search box', async () => {
    renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['ADMIN'] } });
    const user = userEvent.setup();

    await waitFor(() => expect(screen.getByText('Ramesh Agrawal')).toBeInTheDocument());

    await user.type(screen.getByLabelText(/^search/i), 'Goyal');

    await waitFor(() => expect(screen.queryByText('Ramesh Agrawal')).not.toBeInTheDocument());
    expect(screen.getByText('Manoj Goyal')).toBeInTheDocument();
  });

  describe('pagination', () => {
    // The default fixtures only have 2 families - too few to exercise pagination (default page
    // size is 10) - so these tests override the list endpoint with 25 generated families.
    const buildFamilies = (count: number) =>
      Array.from({ length: count }, (_, i) => {
        const base = mockFamilies[0];
        if (!base) throw new Error('fixture data missing');
        return {
          ...base,
          id: `family-page-${i + 1}`,
          familyCode: `FAM-PAGE${String(i + 1).padStart(4, '0')}`,
          headOfFamilyName: `Test Head ${i + 1}`,
        };
      });

    const mockManyFamilies = async (count: number) => {
      const { server } = await import('@/mocks/server');
      const { http, HttpResponse } = await import('msw');
      const many = buildFamilies(count);
      server.use(http.get('*/api/v1/families', () => HttpResponse.json(many)));
      return many;
    };

    it('shows only the first page (10 rows) by default, with sequence numbers starting at 1', async () => {
      const many = await mockManyFamilies(25);
      renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['ADMIN'] } });

      await waitFor(() => expect(screen.getByText(many[0]!.headOfFamilyName)).toBeInTheDocument());

      // Rows 1-10 present, rows 11+ not yet rendered.
      expect(screen.getByText(many[9]!.headOfFamilyName)).toBeInTheDocument();
      expect(screen.queryByText(many[10]!.headOfFamilyName)).not.toBeInTheDocument();

      const rows = screen.getAllByRole('row');
      // First body row (index 1, since index 0 is the header row) should show sequence number 1.
      expect(within(rows[1] as HTMLElement).getByText('1')).toBeInTheDocument();
      expect(within(rows[10] as HTMLElement).getByText('10')).toBeInTheDocument();
    });

    it('advances to the next page and shows the correct sequence numbers', async () => {
      const many = await mockManyFamilies(25);
      renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['ADMIN'] } });

      await waitFor(() => expect(screen.getByText(many[0]!.headOfFamilyName)).toBeInTheDocument());

      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: /next page/i }));

      await waitFor(() => expect(screen.getByText(many[10]!.headOfFamilyName)).toBeInTheDocument());
      expect(screen.queryByText(many[0]!.headOfFamilyName)).not.toBeInTheDocument();

      const rows = screen.getAllByRole('row');
      // Second page starts at sequence number 11.
      expect(within(rows[1] as HTMLElement).getByText('11')).toBeInTheDocument();
    });

    it('resets to the first page when a filter narrows the results', async () => {
      const many = await mockManyFamilies(25);
      renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['ADMIN'] } });

      await waitFor(() => expect(screen.getByText(many[0]!.headOfFamilyName)).toBeInTheDocument());

      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: /next page/i }));
      await waitFor(() => expect(screen.getByText(many[10]!.headOfFamilyName)).toBeInTheDocument());

      // Narrow to a single match, which no longer fills a second page.
      await user.type(screen.getByLabelText(/^search/i), many[0]!.familyCode);

      await waitFor(() => expect(screen.getByText(many[0]!.headOfFamilyName)).toBeInTheDocument());
      expect(screen.queryByText(many[10]!.headOfFamilyName)).not.toBeInTheDocument();
    });

    it('shows more rows per page when the page size is changed', async () => {
      const many = await mockManyFamilies(25);
      renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['ADMIN'] } });

      await waitFor(() => expect(screen.getByText(many[0]!.headOfFamilyName)).toBeInTheDocument());
      expect(screen.queryByText(many[10]!.headOfFamilyName)).not.toBeInTheDocument();

      const user = userEvent.setup();
      const rowsPerPageSelect = screen.getByRole('combobox', { name: /rows per page/i });
      await user.click(rowsPerPageSelect);
      await user.click(await screen.findByRole('option', { name: '25' }));

      // All 25 rows should now be visible on a single page.
      await waitFor(() => expect(screen.getByText(many[24]!.headOfFamilyName)).toBeInTheDocument());
    });
  });

  describe('role-based access to registration', () => {
    it('lets a member who owns no family register one', async () => {
      renderWithProviders(<FamiliesListPage />, {
        // Distinct id: fixture family-1 is owned by the default seedAuthUser id ('test-user'),
        // so this test must NOT share it, or it would look like it already owns a family.
        authUser: { roles: ['MEMBER'], email: 'nobody@example.com', id: 'someone-else' },
      });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /register new family/i })).toBeEnabled();
      });
      expect(screen.queryByText(/already registered a family/i)).not.toBeInTheDocument();
    });

    it('stops a member who already owns a family, and explains why', async () => {
      // fixtures' family-1 is registered under this address, so this user owns one already.
      const owner = mockFamilies[0];
      if (!owner) throw new Error('fixture data missing');

      renderWithProviders(<FamiliesListPage />, {
        authUser: { roles: ['MEMBER'], email: owner.email },
      });

      await waitFor(() => {
        expect(screen.getByText(/already registered a family/i)).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: /register new family/i })).toBeDisabled();
    });

    it('scopes a member to their own family only', async () => {
      const owner = mockFamilies[0];
      if (!owner) throw new Error('fixture data missing');

      renderWithProviders(<FamiliesListPage />, {
        authUser: { roles: ['MEMBER'], email: owner.email },
      });

      await waitFor(() => expect(screen.getByText(owner.headOfFamilyName)).toBeInTheDocument());
      // The other fixture family belongs to someone else and must not be listed.
      expect(screen.queryByText('Manoj Goyal')).not.toBeInTheDocument();
    });

    it('never offers registration to a role without the create permission', async () => {
      // None of the five seeded roles lacks CREATE_FAMILY, so this stands in for a read-only role
      // an administrator creates at runtime - which is the case the permission model exists to
      // support. Overriding /me is how a test expresses "this role's grants", since the frontend
      // has no role table of its own any more.
      const { server } = await import('@/mocks/server');
      const { http, HttpResponse } = await import('msw');
      server.use(
        http.get('*/api/v1/me', () =>
          HttpResponse.json({
            userId: 'read-only-user',
            email: 'auditor@example.com',
            chapterId: 'branch-indore',
            role: { roleId: 'role-auditor', roleCode: 'AUDITOR', roleName: 'Auditor' },
            menus: [],
            permissions: ['VIEW_FAMILY', 'VIEW_ALL_FAMILIES'],
          }),
        ),
      );

      renderWithProviders(<FamiliesListPage />, { authUser: { roles: ['AUDITOR'] } });

      // The auditor reads chapter data, so the table still fills in...
      await waitFor(() => expect(screen.getByText('Ramesh Agrawal')).toBeInTheDocument());
      // ...but the button is absent entirely rather than disabled - there is no cap to explain.
      expect(screen.queryByRole('button', { name: /register new family/i })).not.toBeInTheDocument();
    });

    it('keeps the button available to an admin who already owns a family', async () => {
      const owner = mockFamilies[0];
      if (!owner) throw new Error('fixture data missing');

      renderWithProviders(<FamiliesListPage />, {
        authUser: { roles: ['ADMIN'], email: owner.email },
      });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /register new family/i })).toBeEnabled();
      });
      expect(screen.queryByText(/already registered a family/i)).not.toBeInTheDocument();
    });
  });
});

import { beforeEach, describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen } from '@/test/test-utils';
import { mockFamilies } from '@/mocks/fixtures';
import FamilyRegistrationWizardPage from './FamilyRegistrationWizardPage';
import { clearDraft } from './useFamilyRegistrationDraft';

// The wizard now checks the signed-in user's role and how many families they already own before
// rendering the form (so the one-family cap can't be bypassed by typing the URL). That makes the
// first paint async, hence findBy* rather than getBy* throughout, and every render has to say
// who is looking.
describe('FamilyRegistrationWizardPage', () => {
  beforeEach(() => clearDraft());

  it('renders the wizard page with all required sections', async () => {
    renderWithProviders(<FamilyRegistrationWizardPage />, { authUser: { roles: ['ADMIN'], email: 'admin@example.com' } });

    expect(await screen.findByRole('button', { name: /^next$/i })).toBeInTheDocument();
    expect(screen.getAllByText(/Basic Information/i).length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: /save draft/i })).toBeInTheDocument();
  });

  it('blocks advancing to the next step when required fields are missing', async () => {
    renderWithProviders(<FamilyRegistrationWizardPage />, { authUser: { roles: ['ADMIN'], email: 'admin@example.com' } });
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /^next$/i }));

    // Should show validation errors
    expect(await screen.findAllByText(/required/i)).not.toHaveLength(0);
  });

  it('displays save draft button and allows interaction', async () => {
    renderWithProviders(<FamilyRegistrationWizardPage />, { authUser: { roles: ['ADMIN'], email: 'admin@example.com' } });
    const user = userEvent.setup();

    const saveDraftButton = await screen.findByRole('button', { name: /save draft/i });
    expect(saveDraftButton).toBeInTheDocument();

    // Click save draft button - should not throw an error
    await user.click(saveDraftButton);
  });

  it('refuses to open the form for a member who already owns a family', async () => {
    // Reaching this route directly still has to respect the per-user cap - the route guard only
    // knows roles, so it would happily let this member through to the form.
    const owner = mockFamilies[0];
    if (!owner) throw new Error('fixture data missing');

    renderWithProviders(<FamilyRegistrationWizardPage />, {
      authUser: { roles: ['MEMBER'], email: owner.email },
    });

    expect(await screen.findByText(/already registered a family/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /save draft/i })).not.toBeInTheDocument();
  });

  it('opens the form for a member who owns no family yet', async () => {
    renderWithProviders(<FamilyRegistrationWizardPage />, {
      // Distinct id: fixture family-1 is owned by the default seedAuthUser id ('test-user'), so
      // this test must NOT share it, or it would look like it already owns a family.
      authUser: { roles: ['MEMBER'], email: 'nobody@example.com', id: 'someone-else' },
    });

    expect(await screen.findByRole('button', { name: /^next$/i })).toBeInTheDocument();
    expect(screen.queryByText(/already registered a family/i)).not.toBeInTheDocument();
  });
});

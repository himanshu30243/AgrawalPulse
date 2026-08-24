import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { mockFamilies } from '@/mocks/fixtures';
import MemberRegistrationPage from './MemberRegistrationPage';

describe('MemberRegistrationPage', () => {
  it('shows the missing-family message when no familyId is present in the URL', async () => {
    renderWithProviders(<MemberRegistrationPage />, { route: '/member-registration' });

    expect(await screen.findByText(/no family selected/i)).toBeInTheDocument();
  });

  it('uses router state for the header context and lists existing members when navigated to directly after registration', async () => {
    const family = mockFamilies[0];
    if (!family) throw new Error('fixture data missing');

    renderWithProviders(<MemberRegistrationPage />, {
      route: `/member-registration?familyId=${family.id}`,
      state: { familyId: family.id, familyHeadName: family.headOfFamilyName, gotra: family.gotra, moolGaon: family.nativePlace },
    });

    // Router state is present, so this must not need a network round-trip to show the header.
    // getAllByText, not getByText: the head's name legitimately appears twice once the members
    // table loads - once in the context alert, once as the SELF row (family-1's head is also
    // its own first member).
    await waitFor(() => {
      expect(screen.getAllByText(new RegExp(family.headOfFamilyName)).length).toBeGreaterThan(0);
    });
    // family-1 in fixtures already has 3 members (self/spouse/daughter).
    await waitFor(() => expect(screen.getAllByRole('row').length).toBeGreaterThan(1));
  });

  it('falls back to fetching the family when no router state is present (e.g. a direct visit or refresh)', async () => {
    const family = mockFamilies[1];
    if (!family) throw new Error('fixture data missing');

    renderWithProviders(<MemberRegistrationPage />, {
      route: `/member-registration?familyId=${family.id}`,
    });

    // family-2's one existing member is its own head (SELF), so the name appears both in the
    // fallback-fetched context alert and as a member row - same ambiguity as the router-state test.
    await waitFor(() => {
      expect(screen.getAllByText(new RegExp(family.headOfFamilyName)).length).toBeGreaterThan(0);
    });
  });

  it('adds a new member and shows it in the members list', async () => {
    const family = mockFamilies[1];
    if (!family) throw new Error('fixture data missing');
    renderWithProviders(<MemberRegistrationPage />, {
      route: `/member-registration?familyId=${family.id}`,
      state: { familyId: family.id, familyHeadName: family.headOfFamilyName, gotra: family.gotra, moolGaon: family.nativePlace },
    });
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/^Full Name/i), 'Sunita Goyal');
    fireEvent.change(screen.getByLabelText(/^Date of Birth/i), { target: { value: '1975-06-10' } });
    await user.click(screen.getByRole('button', { name: /^add member$/i }));

    await waitFor(() => expect(screen.getByText('Sunita Goyal')).toBeInTheDocument());
  });
});

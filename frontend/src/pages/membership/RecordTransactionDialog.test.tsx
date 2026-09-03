import { describe, expect, it, vi } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { mockFamilies } from '@/mocks/fixtures';
import MembershipPage from './MembershipPage';
import { RecordTransactionDialog } from './RecordTransactionDialog';

describe('Record Transaction entry point (MembershipPage)', () => {
  it('is hidden for a plain USER (no MANAGE_MEMBERSHIP)', async () => {
    renderWithProviders(<MembershipPage />, { authUser: { roles: ['USER'] } });

    await waitFor(() => expect(screen.getByText(/my status/i)).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: /record transaction/i })).not.toBeInTheDocument();
  });

  it('is visible for a CHAPTER_ADMIN (holds MANAGE_MEMBERSHIP)', async () => {
    renderWithProviders(<MembershipPage />, { authUser: { roles: ['CHAPTER_ADMIN'] } });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /record transaction/i })).toBeInTheDocument();
    });
  });

  it('also shows the admin-only tabs for a CHAPTER_ADMIN but not for a USER', async () => {
    const { unmount } = renderWithProviders(<MembershipPage />, { authUser: { roles: ['USER'] } });
    await waitFor(() => expect(screen.getByText(/my status/i)).toBeInTheDocument());
    expect(screen.queryByText(/pending payment report/i)).not.toBeInTheDocument();
    unmount();

    renderWithProviders(<MembershipPage />, { authUser: { roles: ['CHAPTER_ADMIN'] } });
    await waitFor(() => {
      expect(screen.getByText(/pending payment report/i)).toBeInTheDocument();
    });
  });

  // "My Status" is a member-facing tab - an admin viewing the membership module in an admin
  // capacity has no "own" status here and must not see it at all, not just default to another tab.
  it('hides the "My Status" tab entirely for admin roles, but shows it for a plain USER', async () => {
    const { unmount } = renderWithProviders(<MembershipPage />, { authUser: { roles: ['USER'] } });
    await waitFor(() => expect(screen.getByRole('tab', { name: /my status/i })).toBeInTheDocument());
    unmount();

    for (const role of ['CHAPTER_ADMIN', 'STATE_ADMIN', 'NATIONAL_ADMIN'] as const) {
      const rendered = renderWithProviders(<MembershipPage />, { authUser: { roles: [role] } });
      await waitFor(() => expect(screen.getByRole('tab', { name: /members/i })).toBeInTheDocument());
      expect(screen.queryByRole('tab', { name: /my status/i })).not.toBeInTheDocument();
      rendered.unmount();
    }
  });
});

describe('RecordTransactionDialog', () => {
  it('records a new transaction directly when the family is already known (no search step)', async () => {
    const family = mockFamilies[0];
    if (!family) throw new Error('fixture data missing');
    const onSaved = vi.fn();

    renderWithProviders(
      <RecordTransactionDialog initialFamily={family} onClose={() => undefined} onSaved={onSaved} />,
      { authUser: { roles: ['CHAPTER_ADMIN'] } },
    );

    // Search step is skipped - the family is shown directly.
    expect(await screen.findByText(new RegExp(family.familyCode))).toBeInTheDocument();
    expect(screen.queryByLabelText(/search family/i)).not.toBeInTheDocument();

    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/^amount/i), '250');
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });

  it('rejects Save with a clear message when Amount is left blank, and highlights the field', async () => {
    const family = mockFamilies[0];
    if (!family) throw new Error('fixture data missing');
    const onSaved = vi.fn();

    renderWithProviders(
      <RecordTransactionDialog initialFamily={family} onClose={() => undefined} onSaved={onSaved} />,
      { authUser: { roles: ['CHAPTER_ADMIN'] } },
    );

    const amountField = await screen.findByLabelText(/^amount/i);
    const user = userEvent.setup();
    // Amount left blank - clicking Save must not be a silent no-op (the button itself is not
    // disabled just because Amount is empty - see RecordTransactionDialog's canSave comment).
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    expect(await screen.findByText(/amount is required/i)).toBeInTheDocument();
    expect(amountField).toHaveAttribute('aria-invalid', 'true');
    expect(onSaved).not.toHaveBeenCalled();

    // The message clears once the user starts fixing it.
    await user.type(amountField, '250');
    expect(screen.queryByText(/amount is required/i)).not.toBeInTheDocument();
  });

  it('rejects a zero or negative amount the same way as a blank one', async () => {
    const family = mockFamilies[0];
    if (!family) throw new Error('fixture data missing');

    renderWithProviders(
      <RecordTransactionDialog initialFamily={family} onClose={() => undefined} onSaved={() => undefined} />,
      { authUser: { roles: ['CHAPTER_ADMIN'] } },
    );
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^amount/i), '0');
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    expect(await screen.findByText(/amount is required/i)).toBeInTheDocument();
  });

  it('search-then-create: finds a family by name before showing the payment fields', async () => {
    const family = mockFamilies[0];
    if (!family) throw new Error('fixture data missing');

    renderWithProviders(<RecordTransactionDialog onClose={() => undefined} onSaved={() => undefined} />, {
      authUser: { roles: ['CHAPTER_ADMIN'] },
    });
    const user = userEvent.setup();

    // Payment fields aren't shown until a family is picked.
    expect(screen.queryByLabelText(/^amount/i)).not.toBeInTheDocument();

    await user.type(screen.getByPlaceholderText(/search by head of family/i), family.headOfFamilyName);
    await user.click(screen.getByRole('button', { name: /^search$/i }));

    const result = await screen.findByText(family.headOfFamilyName);
    await user.click(result);

    expect(await screen.findByLabelText(/^amount/i)).toBeInTheDocument();
  });
});

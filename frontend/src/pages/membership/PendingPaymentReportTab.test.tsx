import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { mockFamilies } from '@/mocks/fixtures';
import { PendingPaymentReportTab } from './PendingPaymentReportTab';

// family-1 (Ramesh Agrawal) has one seed transaction for last FY -> PENDING_RENEWAL this FY.
// family-2 (Manoj Goyal) has none -> EXPIRED. Neither is ACTIVE, so both appear in the report by
// default (see handlers.ts's pendingPaymentReport handler, which excludes ACTIVE only).
describe('PendingPaymentReportTab', () => {
  it('lists every non-active family by default', async () => {
    renderWithProviders(<PendingPaymentReportTab />, { authUser: { roles: ['CHAPTER_ADMIN'] } });

    await waitFor(() => {
      expect(screen.getByText('Ramesh Agrawal')).toBeInTheDocument();
    });
    expect(screen.getByText('Manoj Goyal')).toBeInTheDocument();
  });

  it('narrows by head of family name (backend-composed filter)', async () => {
    renderWithProviders(<PendingPaymentReportTab />, { authUser: { roles: ['CHAPTER_ADMIN'] } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Manoj Goyal')).toBeInTheDocument());

    await user.type(screen.getByLabelText(/head of family/i), 'Goyal');

    await waitFor(() => expect(screen.queryByText('Ramesh Agrawal')).not.toBeInTheDocument());
    expect(screen.getByText('Manoj Goyal')).toBeInTheDocument();
  });

  it('narrows by mobile number (backend-composed filter)', async () => {
    const family = mockFamilies[0];
    if (!family) throw new Error('fixture data missing');
    renderWithProviders(<PendingPaymentReportTab />, { authUser: { roles: ['CHAPTER_ADMIN'] } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Manoj Goyal')).toBeInTheDocument());

    await user.type(screen.getByLabelText(/mobile number/i), family.mobileNumber);

    await waitFor(() => expect(screen.queryByText('Manoj Goyal')).not.toBeInTheDocument());
    expect(screen.getByText('Ramesh Agrawal')).toBeInTheDocument();
  });

  it('narrows by area/locality (backend-composed filter)', async () => {
    renderWithProviders(<PendingPaymentReportTab />, { authUser: { roles: ['CHAPTER_ADMIN'] } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Manoj Goyal')).toBeInTheDocument());

    await user.type(screen.getByLabelText(/area \/ locality/i), 'Palasia');

    await waitFor(() => expect(screen.queryByText('Ramesh Agrawal')).not.toBeInTheDocument());
    expect(screen.getByText('Manoj Goyal')).toBeInTheDocument();
  });

  it('narrows by family ID text (backend-composed filter, matched against familyCode)', async () => {
    const family = mockFamilies[0];
    if (!family) throw new Error('fixture data missing');
    renderWithProviders(<PendingPaymentReportTab />, { authUser: { roles: ['CHAPTER_ADMIN'] } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Manoj Goyal')).toBeInTheDocument());

    await user.type(screen.getByLabelText(/family id/i), family.familyCode);

    await waitFor(() => expect(screen.queryByText('Manoj Goyal')).not.toBeInTheDocument());
    expect(screen.getByText('Ramesh Agrawal')).toBeInTheDocument();
  });

  it('narrows by Membership Status client-side (EXPIRED excludes the PENDING_RENEWAL family)', async () => {
    renderWithProviders(<PendingPaymentReportTab />, { authUser: { roles: ['CHAPTER_ADMIN'] } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Ramesh Agrawal')).toBeInTheDocument());

    await user.click(screen.getByLabelText(/^status$/i));
    await user.click(await screen.findByRole('option', { name: /^expired$/i }));

    await waitFor(() => expect(screen.queryByText('Ramesh Agrawal')).not.toBeInTheDocument());
    expect(screen.getByText('Manoj Goyal')).toBeInTheDocument();
  });
});

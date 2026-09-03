import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { mockFamilies } from '@/mocks/fixtures';
import { MembershipStatusTab } from './MembershipStatusTab';

describe('MembershipStatusTab', () => {
  it('shows a no-family message rather than another family when the caller owns none', async () => {
    renderWithProviders(<MembershipStatusTab />, {
      authUser: { roles: ['USER'], email: 'nobody@example.com', id: 'someone-else' },
    });

    await waitFor(() => {
      expect(screen.getByText(/don't have a registered family/i)).toBeInTheDocument();
    });
    // Neither fixture family's data should render for a caller who owns neither.
    expect(screen.queryByText(mockFamilies[0]?.headOfFamilyName ?? '')).not.toBeInTheDocument();
    expect(screen.queryByText(mockFamilies[1]?.headOfFamilyName ?? '')).not.toBeInTheDocument();
  });

  it("shows the caller's own status and transaction history (requirement #1)", async () => {
    const owner = mockFamilies[0];
    if (!owner) throw new Error('fixture data missing');

    renderWithProviders(<MembershipStatusTab />, {
      authUser: { roles: ['USER'], email: owner.email },
    });

    // The mock seeds family-1 with one transaction for last FY - grace period puts it at
    // PENDING_RENEWAL (see handlers.ts's computeMockStatus).
    await waitFor(() => {
      expect(screen.getByText(/pending renewal/i)).toBeInTheDocument();
    });
    expect(screen.getByText('TXN-SEED-1')).toBeInTheDocument();
  });

  // The requirement #1 isolation regression test: the other fixture family's data must never
  // appear on this page, even though it legitimately exists in the mock backend.
  it("never renders another family's data", async () => {
    const owner = mockFamilies[0];
    const other = mockFamilies[1];
    if (!owner || !other) throw new Error('fixture data missing');

    renderWithProviders(<MembershipStatusTab />, {
      authUser: { roles: ['USER'], email: owner.email },
    });

    await waitFor(() => {
      expect(screen.getByText('TXN-SEED-1')).toBeInTheDocument();
    });
    expect(screen.queryByText(other.headOfFamilyName)).not.toBeInTheDocument();
    expect(screen.queryByText(other.familyCode)).not.toBeInTheDocument();
  });
});

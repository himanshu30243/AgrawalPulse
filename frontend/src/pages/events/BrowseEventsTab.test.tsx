import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor, within } from '@/test/test-utils';
import { mockFamilies } from '@/mocks/fixtures';
import { BrowseEventsTab } from './BrowseEventsTab';

// Fixture recap (see mocks/fixtures.ts's mockEvents): event-1 "Diwali Milan" (branch-indore,
// PUBLISHED, upcoming), event-2 "Youth Career Fair" (branch-indore, DRAFT), event-3 "Holi
// Celebration" (branch-bhopal, PUBLISHED, upcoming), event-4 (branch-indore, CANCELLED),
// event-5 "Past Annual Meet" (branch-indore, PUBLISHED, past). Default seedAuthUser chapterId is
// 'branch-indore', so a plain USER's own-chapter floor only ever includes indore events.
describe('BrowseEventsTab', () => {
  it('shows only published, upcoming, own-chapter events by default', async () => {
    renderWithProviders(<BrowseEventsTab />, { authUser: { roles: ['USER'] } });

    await waitFor(() => expect(screen.getByText('Diwali Milan')).toBeInTheDocument());
    // Draft, cancelled, another chapter's event, and a past event must not appear by default.
    expect(screen.queryByText('Youth Career Fair')).not.toBeInTheDocument();
    expect(screen.queryByText('Cancelled Blood Donation Camp')).not.toBeInTheDocument();
    expect(screen.queryByText('Holi Celebration')).not.toBeInTheDocument();
    expect(screen.queryByText('Past Annual Meet')).not.toBeInTheDocument();
  });

  it('switching to Past shows past published events instead', async () => {
    renderWithProviders(<BrowseEventsTab />, { authUser: { roles: ['USER'] } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Diwali Milan')).toBeInTheDocument());

    await user.click(screen.getByLabelText(/when/i));
    await user.click(await screen.findByRole('option', { name: /^past$/i }));

    await waitFor(() => expect(screen.getByText('Past Annual Meet')).toBeInTheDocument());
    expect(screen.queryByText('Diwali Milan')).not.toBeInTheDocument();
  });

  it('filters by search keyword', async () => {
    renderWithProviders(<BrowseEventsTab />, { authUser: { roles: ['USER'] } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Diwali Milan')).toBeInTheDocument());

    await user.type(screen.getByLabelText(/^search/i), 'nonexistent-keyword');

    await waitFor(() => expect(screen.queryByText('Diwali Milan')).not.toBeInTheDocument());
    expect(screen.getByText(/no events match/i)).toBeInTheDocument();
  });

  it('opens the detail dialog and registers the caller\'s own family', async () => {
    const owner = mockFamilies[0];
    if (!owner) throw new Error('fixture data missing');

    renderWithProviders(<BrowseEventsTab />, { authUser: { roles: ['USER'], email: owner.email } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Diwali Milan')).toBeInTheDocument());

    await user.click(screen.getByText('Diwali Milan'));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: /^register$/i }));

    expect(await screen.findByText(/registered for this event/i)).toBeInTheDocument();
  });
});

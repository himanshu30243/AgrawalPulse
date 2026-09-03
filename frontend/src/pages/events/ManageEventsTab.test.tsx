import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor, within } from '@/test/test-utils';
import { ManageEventsTab } from './ManageEventsTab';
import EventsPage from './EventsPage';

// Admin management surface - unlike BrowseEventsTab, shows every status by default (see
// mocks/fixtures.ts's mockEvents: event-1 PUBLISHED, event-2 DRAFT, event-4 CANCELLED, all
// branch-indore, matching the default seedAuthUser chapterId).
describe('ManageEventsTab', () => {
  it('lists events of every status, not just published, within the caller\'s own chapter', async () => {
    renderWithProviders(<ManageEventsTab />, { authUser: { roles: ['CHAPTER_ADMIN'] } });

    await waitFor(() => expect(screen.getByText('Diwali Milan')).toBeInTheDocument());
    expect(screen.getByText('Youth Career Fair')).toBeInTheDocument();
    expect(screen.getByText('Cancelled Blood Donation Camp')).toBeInTheDocument();
    // Different chapter - still out of a CHAPTER_ADMIN's own-chapter scope in the mock.
    expect(screen.queryByText('Holi Celebration')).not.toBeInTheDocument();
  });

  it('filters by status', async () => {
    renderWithProviders(<ManageEventsTab />, { authUser: { roles: ['CHAPTER_ADMIN'] } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Youth Career Fair')).toBeInTheDocument());

    await user.click(screen.getByLabelText(/^status$/i));
    await user.click(await screen.findByRole('option', { name: /^draft$/i }));

    await waitFor(() => expect(screen.queryByText('Diwali Milan')).not.toBeInTheDocument());
    expect(screen.getByText('Youth Career Fair')).toBeInTheDocument();
  });

  it('publishes a draft event', async () => {
    renderWithProviders(<ManageEventsTab />, { authUser: { roles: ['CHAPTER_ADMIN'] } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Youth Career Fair')).toBeInTheDocument());

    const row = screen.getByText('Youth Career Fair').closest('tr');
    if (!row) throw new Error('row not found');
    await user.click(within(row).getByRole('button', { name: /^publish$/i }));

    await waitFor(() => {
      expect(within(row).getByText(/^published$/i)).toBeInTheDocument();
    });
  });

  it('deletes an event after confirmation', async () => {
    renderWithProviders(<ManageEventsTab />, { authUser: { roles: ['CHAPTER_ADMIN'] } });
    const user = userEvent.setup();
    await waitFor(() => expect(screen.getByText('Cancelled Blood Donation Camp')).toBeInTheDocument());

    const row = screen.getByText('Cancelled Blood Donation Camp').closest('tr');
    if (!row) throw new Error('row not found');
    await user.click(within(row).getByRole('button', { name: /^delete$/i }));

    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: /^delete$/i }));

    await waitFor(() => {
      expect(screen.queryByText('Cancelled Blood Donation Camp')).not.toBeInTheDocument();
    });
  });
});

describe('Manage Events tab visibility', () => {
  it('is hidden for a plain USER and visible for a CHAPTER_ADMIN', async () => {
    const { unmount } = renderWithProviders(<EventsPage />, { authUser: { roles: ['USER'] } });
    await waitFor(() => expect(screen.getByText(/browse events/i)).toBeInTheDocument());
    expect(screen.queryByText(/manage events/i)).not.toBeInTheDocument();
    unmount();

    renderWithProviders(<EventsPage />, { authUser: { roles: ['CHAPTER_ADMIN'] } });
    await waitFor(() => expect(screen.getByText(/manage events/i)).toBeInTheDocument());
  });
});

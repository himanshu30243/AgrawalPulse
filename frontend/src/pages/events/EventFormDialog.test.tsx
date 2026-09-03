import { describe, expect, it, vi } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { mockEvents } from '@/mocks/fixtures';
import { EventFormDialog } from './EventFormDialog';

describe('EventFormDialog', () => {
  it('rejects Save with a blank title, highlighting the field', async () => {
    renderWithProviders(<EventFormDialog onClose={() => undefined} onSaved={() => undefined} />, {
      authUser: { roles: ['CHAPTER_ADMIN'] },
    });
    const user = userEvent.setup();

    const titleField = screen.getByLabelText(/event title/i);
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    expect(await screen.findByText(/event title is required/i)).toBeInTheDocument();
    expect(titleField).toHaveAttribute('aria-invalid', 'true');
  });

  it('rejects a start time that is not before the end time', async () => {
    renderWithProviders(<EventFormDialog onClose={() => undefined} onSaved={() => undefined} />, {
      authUser: { roles: ['CHAPTER_ADMIN'] },
    });
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/event title/i), 'Diwali Milan');
    const startTime = screen.getByLabelText(/start time/i);
    const endTime = screen.getByLabelText(/end time/i);
    await user.clear(startTime);
    await user.type(startTime, '15:00');
    await user.clear(endTime);
    await user.type(endTime, '10:00');

    await user.click(screen.getByRole('button', { name: /^save$/i }));

    expect(await screen.findByText(/start time must be earlier than end time/i)).toBeInTheDocument();
  });

  it('rejects an event date in the past', async () => {
    renderWithProviders(<EventFormDialog onClose={() => undefined} onSaved={() => undefined} />, {
      authUser: { roles: ['CHAPTER_ADMIN'] },
    });
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/event title/i), 'Diwali Milan');
    const dateField = screen.getByLabelText(/^date/i);
    await user.clear(dateField);
    await user.type(dateField, '2020-01-01');

    await user.click(screen.getByRole('button', { name: /^save$/i }));

    expect(await screen.findByText(/event date cannot be in the past/i)).toBeInTheDocument();
  });

  it('creates a new event with valid data', async () => {
    const onSaved = vi.fn();
    renderWithProviders(<EventFormDialog onClose={() => undefined} onSaved={onSaved} />, {
      authUser: { roles: ['CHAPTER_ADMIN'] },
    });
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/event title/i), 'Diwali Milan');
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });

  it('pre-fills fields when editing an existing event', async () => {
    const event = mockEvents[0];
    if (!event) throw new Error('fixture data missing');

    renderWithProviders(<EventFormDialog event={event} onClose={() => undefined} onSaved={() => undefined} />, {
      authUser: { roles: ['CHAPTER_ADMIN'] },
    });

    expect(screen.getByLabelText(/event title/i)).toHaveValue(event.title);
    expect(screen.getByText(/edit event/i)).toBeInTheDocument();
  });
});

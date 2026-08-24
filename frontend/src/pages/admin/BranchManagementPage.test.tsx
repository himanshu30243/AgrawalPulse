import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor, within } from '@/test/test-utils';
import BranchManagementPage from './BranchManagementPage';

const ADMIN = { authUser: { roles: ['ADMIN'] } };

describe('BranchManagementPage', () => {
  it('lists branches from the backend', async () => {
    renderWithProviders(<BranchManagementPage />, ADMIN);

    await waitFor(() => expect(screen.getByText('Indore Chapter')).toBeInTheDocument());
    expect(screen.getByText('Bhopal Chapter')).toBeInTheDocument();
  });

  it('creates a branch through the API', async () => {
    renderWithProviders(<BranchManagementPage />, ADMIN);
    const user = userEvent.setup({ delay: null });

    await waitFor(() => expect(screen.getByText('Indore Chapter')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /create branch/i }));

    const dialog = await screen.findByRole('dialog');
    await user.type(within(dialog).getByLabelText(/name/i), 'Gwalior Chapter');
    await user.type(within(dialog).getByLabelText(/city/i), 'Gwalior');
    await user.type(within(dialog).getByLabelText(/state/i), 'Madhya Pradesh');
    await user.click(within(dialog).getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(screen.getByText('Gwalior Chapter')).toBeInTheDocument());
  });

  it('never offers deletion, because chapter_id is the tenant boundary for five services', async () => {
    renderWithProviders(<BranchManagementPage />, ADMIN);

    await waitFor(() => expect(screen.getByText('Indore Chapter')).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument();
    // ...and says so, rather than leaving the absence unexplained.
    expect(screen.getByText(/cannot be deleted/i)).toBeInTheDocument();
  });

  it('validates that all three fields are present before calling the API', async () => {
    renderWithProviders(<BranchManagementPage />, ADMIN);
    const user = userEvent.setup({ delay: null });

    await waitFor(() => expect(screen.getByText('Indore Chapter')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /create branch/i }));

    const dialog = await screen.findByRole('dialog');
    await user.type(within(dialog).getByLabelText(/name/i), 'Incomplete');
    await user.click(within(dialog).getByRole('button', { name: /^save$/i }));

    expect(await within(dialog).findByRole('alert')).toBeInTheDocument();
  });
});

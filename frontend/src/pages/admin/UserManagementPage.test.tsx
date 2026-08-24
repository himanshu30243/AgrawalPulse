import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor, within } from '@/test/test-utils';
import UserManagementPage from './UserManagementPage';

const ADMIN = { authUser: { roles: ['ADMIN'] } };

describe('UserManagementPage', () => {
  it('lists users with the single role each one holds', async () => {
    renderWithProviders(<UserManagementPage />, ADMIN);

    await waitFor(() => expect(screen.getByText('ramesh.agrawal@example.com')).toBeInTheDocument());
    expect(screen.getByText('chapter.admin@example.com')).toBeInTheDocument();
    expect(screen.getByText('Chapter Admin')).toBeInTheDocument();
  });

  it('changes a user’s role through the API', async () => {
    renderWithProviders(<UserManagementPage />, ADMIN);
    const user = userEvent.setup();

    await waitFor(() => expect(screen.getByText('ramesh.agrawal@example.com')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /change role for ramesh\.agrawal@example\.com/i }));

    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('combobox', { name: /role/i }));
    await user.click(await screen.findByRole('option', { name: 'State Admin' }));
    await user.click(within(dialog).getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await waitFor(() => expect(screen.getByText('State Admin')).toBeInTheDocument());
  });

  it('offers one role, not a set - a user holds exactly one since the V2 migration', async () => {
    renderWithProviders(<UserManagementPage />, ADMIN);
    const user = userEvent.setup();

    await waitFor(() => expect(screen.getByText('ramesh.agrawal@example.com')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /change role for ramesh\.agrawal@example\.com/i }));

    const dialog = await screen.findByRole('dialog');
    // A multi-select would render checkboxes; a single select renders a combobox.
    expect(within(dialog).getByRole('combobox', { name: /role/i })).toBeInTheDocument();
    expect(within(dialog).queryAllByRole('checkbox')).toHaveLength(0);
  });
});

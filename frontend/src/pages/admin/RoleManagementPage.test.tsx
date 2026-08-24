import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor, within } from '@/test/test-utils';
import RoleManagementPage from './RoleManagementPage';

const ADMIN = { authUser: { roles: ['ADMIN'] } };

describe('RoleManagementPage', () => {
  it('lists roles from the backend with their grant counts', async () => {
    renderWithProviders(<RoleManagementPage />, ADMIN);

    await waitFor(() => expect(screen.getByText('Chapter Admin')).toBeInTheDocument());
    // The code is shown alongside the display name - it's what @PreAuthorize and the JWT use.
    expect(screen.getByText('CHAPTER_ADMIN')).toBeInTheDocument();
    expect(screen.getByText('Administrator')).toBeInTheDocument();
  });

  it('creates a role through the API and shows it in the table', async () => {
    renderWithProviders(<RoleManagementPage />, ADMIN);
    // delay: null removes userEvent's per-keystroke delay. These dialogs have several text
    // fields, and at the default delay the typing alone outlasts the assertion timeout once the
    // full suite is running in parallel - the tests passed alone and failed together.
    const user = userEvent.setup({ delay: null });

    await waitFor(() => expect(screen.getByText('Administrator')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /create role/i }));

    await user.type(screen.getByLabelText(/role code/i), 'auditor');
    await user.type(screen.getByLabelText(/role name/i), 'Auditor');
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(screen.getByText('Auditor')).toBeInTheDocument());
    // Codes are upper-cased before being sent, so the round-tripped row shows AUDITOR.
    expect(screen.getByText('AUDITOR')).toBeInTheDocument();
  });

  it('surfaces the server error instead of silently closing when creation is rejected', async () => {
    renderWithProviders(<RoleManagementPage />, ADMIN);
    // delay: null removes userEvent's per-keystroke delay. These dialogs have several text
    // fields, and at the default delay the typing alone outlasts the assertion timeout once the
    // full suite is running in parallel - the tests passed alone and failed together.
    const user = userEvent.setup({ delay: null });

    await waitFor(() => expect(screen.getByText('Administrator')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /create role/i }));

    // USER already exists - the mock backend rejects it exactly as user-service does.
    await user.type(screen.getByLabelText(/role code/i), 'USER');
    await user.type(screen.getByLabelText(/role name/i), 'Duplicate');
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    const dialog = await screen.findByRole('dialog');
    await waitFor(() => expect(within(dialog).getByRole('alert')).toBeInTheDocument());
    // The dialog must stay open so the entered values aren't lost.
    expect(within(dialog).getByLabelText(/role name/i)).toBeInTheDocument();
  });

  it('refuses to let a role code be edited once it exists', async () => {
    renderWithProviders(<RoleManagementPage />, ADMIN);
    // delay: null removes userEvent's per-keystroke delay. These dialogs have several text
    // fields, and at the default delay the typing alone outlasts the assertion timeout once the
    // full suite is running in parallel - the tests passed alone and failed together.
    const user = userEvent.setup({ delay: null });

    await waitFor(() => expect(screen.getByText('Chapter Admin')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /edit chapter admin/i }));

    // Renaming a code would strip access from everyone holding it, since the code is what the
    // JWT and every @PreAuthorize reference.
    expect(await screen.findByLabelText(/role code/i)).toBeDisabled();
    expect(screen.getByLabelText(/role name/i)).toBeEnabled();
  });

  it('saves a menu assignment, which is what drives that role’s dashboard nav', async () => {
    renderWithProviders(<RoleManagementPage />, ADMIN);
    // delay: null removes userEvent's per-keystroke delay. These dialogs have several text
    // fields, and at the default delay the typing alone outlasts the assertion timeout once the
    // full suite is running in parallel - the tests passed alone and failed together.
    const user = userEvent.setup({ delay: null });

    await waitFor(() => expect(screen.getByText('User')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /menus for user/i }));

    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('checkbox', { name: /reports/i }));
    await user.click(within(dialog).getByRole('button', { name: /save menus/i }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
});

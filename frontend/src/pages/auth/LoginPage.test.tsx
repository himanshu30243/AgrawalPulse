import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import LoginPage from './LoginPage';

describe('LoginPage', () => {
  it('asks for nothing but credentials', async () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByLabelText(/email or mobile number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();

    // Chapter and role belong to the user's account, not to a sign-in form. The server resolves
    // both from the account record, so neither is asked for here.
    expect(screen.queryByLabelText(/chapter/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/roles?/i)).not.toBeInTheDocument();
  });

  it('goes straight to the dashboard on success, with no interstitial', async () => {
    renderWithProviders(<LoginPage />, {
      extraRoutes: { '/dashboard': <div>DASHBOARD</div> },
    });
    const user = userEvent.setup({ delay: null });

    await user.type(screen.getByLabelText(/email or mobile number/i), 'ramesh.agrawal@example.com');
    await user.type(screen.getByLabelText(/^password$/i), 'password123');
    await user.click(screen.getByRole('button', { name: /^login$/i }));

    expect(await screen.findByText('DASHBOARD')).toBeInTheDocument();
    // The old flow parked the user on a "Login Successful!" card for 1.5s first.
    expect(screen.queryByText(/login successful/i)).not.toBeInTheDocument();
  });

  it('accepts a mobile number too - the backend detects which identifier this is', async () => {
    renderWithProviders(<LoginPage />, {
      extraRoutes: { '/dashboard': <div>DASHBOARD</div> },
    });
    const user = userEvent.setup({ delay: null });

    await user.type(screen.getByLabelText(/email or mobile number/i), '9876500001');
    await user.type(screen.getByLabelText(/^password$/i), 'password123');
    await user.click(screen.getByRole('button', { name: /^login$/i }));

    expect(await screen.findByText('DASHBOARD')).toBeInTheDocument();
  });

  it('rejects an unregistered account', async () => {
    renderWithProviders(<LoginPage />);
    const user = userEvent.setup({ delay: null });

    await user.type(screen.getByLabelText(/email or mobile number/i), 'nobody@example.com');
    await user.type(screen.getByLabelText(/^password$/i), 'password123');
    await user.click(screen.getByRole('button', { name: /^login$/i }));

    expect(await screen.findByText(/does not exist/i)).toBeInTheDocument();
  });

  it('rejects the wrong password', async () => {
    renderWithProviders(<LoginPage />);
    const user = userEvent.setup({ delay: null });

    await user.type(screen.getByLabelText(/email or mobile number/i), 'ramesh.agrawal@example.com');
    await user.type(screen.getByLabelText(/^password$/i), 'wrongpassword');
    await user.click(screen.getByRole('button', { name: /^login$/i }));

    expect(await screen.findByText(/invalid username or password/i)).toBeInTheDocument();
  });

  it('validates before calling the API', async () => {
    renderWithProviders(<LoginPage />);
    const user = userEvent.setup({ delay: null });

    await user.type(screen.getByLabelText(/email or mobile number/i), 'demo@example.com');
    await user.type(screen.getByLabelText(/^password$/i), 'short');
    await user.click(screen.getByRole('button', { name: /^login$/i }));

    await waitFor(() =>
      expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument(),
    );
  });
});

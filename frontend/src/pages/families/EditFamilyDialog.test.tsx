import { describe, expect, it, vi } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { EditFamilyDialog } from './EditFamilyDialog';
import { mockFamilies } from '@/mocks/fixtures';

describe('EditFamilyDialog', () => {
  const family = mockFamilies[0];
  if (!family) throw new Error('fixture data missing');

  it('pre-fills the form with the family\'s current head/contact/location details', () => {
    renderWithProviders(<EditFamilyDialog family={family} onClose={vi.fn()} onSaved={vi.fn()} />);

    expect(screen.getByLabelText('First Name')).toHaveValue(family.headFirstName);
    expect(screen.getByLabelText('Last Name')).toHaveValue(family.headLastName);
    expect(screen.getByLabelText('Mobile Number')).toHaveValue(family.mobileNumber);
    expect(screen.getByLabelText('Email')).toHaveValue(family.email);
    expect(screen.getByLabelText('State')).toHaveValue(family.state);
    expect(screen.getByLabelText('City')).toHaveValue(family.district);
  });

  it('saves edited head details and reports the updated family back to the caller', async () => {
    const onSaved = vi.fn();
    renderWithProviders(<EditFamilyDialog family={family} onClose={vi.fn()} onSaved={onSaved} />);
    const user = userEvent.setup();

    const firstName = screen.getByLabelText('First Name');
    await user.clear(firstName);
    await user.type(firstName, 'Suresh');

    await user.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
    expect(onSaved.mock.calls[0]?.[0]).toMatchObject({ headFirstName: 'Suresh' });
  });

  it('blocks saving an invalid mobile number and never calls onSaved', async () => {
    const onSaved = vi.fn();
    renderWithProviders(<EditFamilyDialog family={family} onClose={vi.fn()} onSaved={onSaved} />);
    const user = userEvent.setup();

    const mobile = screen.getByLabelText('Mobile Number');
    await user.clear(mobile);
    await user.type(mobile, '12345');

    await user.click(screen.getByRole('button', { name: /^save$/i }));

    expect(await screen.findByText(/enter a valid 10-digit mobile number/i)).toBeInTheDocument();
    expect(onSaved).not.toHaveBeenCalled();
  });

  it('auto-fills State/City from a known PIN code, same as the registration wizard', async () => {
    renderWithProviders(<EditFamilyDialog family={family} onClose={vi.fn()} onSaved={vi.fn()} />);
    const user = userEvent.setup();

    // The mock handler (src/mocks/handlers.ts) resolves 462001 -> Bhopal, Madhya Pradesh.
    const pinField = screen.getByLabelText('PIN Code');
    await user.clear(pinField);
    await user.type(pinField, '462001');

    await waitFor(() => expect(screen.getByLabelText('City')).toHaveValue('Bhopal'));
    expect(screen.getByLabelText('State')).toHaveValue('Madhya Pradesh');
  });
});

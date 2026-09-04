import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { AddressStep } from './AddressStep';
import { emptyWizardState } from '../types';
import type { WizardFormErrors, WizardFormState } from '../types';

// A thin stateful harness - AddressStep is a controlled component (values/setField/errors come
// from its parent wizard page), so exercising the real state-update path needs a real setState
// here rather than a static prop object that never actually changes.
function Harness() {
  const [values, setValues] = useState<WizardFormState>(emptyWizardState());
  const errors: WizardFormErrors = {};
  const setField = <K extends keyof WizardFormState>(key: K, value: WizardFormState[K]) => {
    setValues((prev) => ({ ...prev, [key]: value }));
  };
  return <AddressStep values={values} errors={errors} setField={setField} />;
}

describe('AddressStep', () => {
  it('auto-fills State and District once a known PIN code is fully typed', async () => {
    renderWithProviders(<Harness />);
    const user = userEvent.setup();

    // The mock handler (src/mocks/handlers.ts) resolves 452001 -> Indore, Madhya Pradesh.
    await user.type(screen.getByPlaceholderText('302001'), '452001');

    await waitFor(() => expect(screen.getByDisplayValue('Madhya Pradesh')).toBeInTheDocument());
    expect(screen.getByDisplayValue('Indore')).toBeInTheDocument();
    expect(screen.getByText(/filled in automatically/i)).toBeInTheDocument();
  });

  it('falls back to the manual State/District dropdowns for an unknown PIN code', async () => {
    renderWithProviders(<Harness />);
    const user = userEvent.setup();

    await user.type(screen.getByPlaceholderText('302001'), '999999');
    await waitFor(() => expect(screen.getByText(/could not look up this pin code/i)).toBeInTheDocument());

    // The dropdown fallback is still present and usable, not a dead end - MUI's Select has no
    // accessible name here (StepLabel is a plain, unassociated Typography, not a <label for>), so
    // these are found positionally: Country, State, District.
    const comboboxes = screen.getAllByRole('combobox');
    expect(comboboxes).toHaveLength(3);
    await user.click(comboboxes[1] as HTMLElement);
    expect(await screen.findByRole('option', { name: 'Madhya Pradesh' })).toBeInTheDocument();
  });

  it('switches State/District/PIN to free text for a non-India country', async () => {
    renderWithProviders(<Harness />);
    const user = userEvent.setup();

    const comboboxes = screen.getAllByRole('combobox');
    await user.click(comboboxes[0] as HTMLElement); // Country
    await user.click(await screen.findByRole('option', { name: 'United States' }));

    expect(screen.getByPlaceholderText('State / Province')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('City')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Postal code')).toBeInTheDocument();
    // The India-only 6-digit PIN field/label is gone, not just relabeled.
    expect(screen.queryByPlaceholderText('302001')).not.toBeInTheDocument();
  });
});

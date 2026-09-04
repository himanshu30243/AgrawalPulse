import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import RegistrationPage from './RegistrationPage';

describe('RegistrationPage', () => {
  it('tells the registrant to sign up as Head of Family only, not one account per member', () => {
    renderWithProviders(<RegistrationPage />);

    expect(
      screen.getByText(/please register only as the head of family/i),
    ).toBeInTheDocument();
  });
});

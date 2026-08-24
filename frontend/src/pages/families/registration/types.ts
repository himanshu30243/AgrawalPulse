import type { FamilyCategory, Gender, Samaj } from '@/types/domain';

// Mirrors CreateFamilyRequest's fields (see @/types/domain) plus a few UI-only concerns that
// never go to the backend directly: `photo` (uploaded separately after the family exists -
// see FamilyRegistrationWizardPage's submit sequence), and `otherGotra` (the free-text typed
// when gotra === 'Other', resolved into the final `gotra` string before submit).
export interface WizardFormState {
  headFirstName: string;
  headMiddleName: string;
  headLastName: string;
  familyName: string;
  headGender: Gender | '';
  headDateOfBirth: string;
  mobileNumber: string;
  email: string;
  aadhaarNumber: string;
  photo: File | null;

  address: string;
  country: string;
  state: string;
  district: string;
  areaLocality: string;
  pinCode: string;

  samaj: Samaj | '';
  gotra: string;
  otherGotra: string;
  nativePlace: string;

  occupationBusinessType: string;
  annualIncomeRange: string;
  familyCategory: FamilyCategory | '';
  ownTwoWheeler: boolean;
  ownFourWheeler: boolean;
  ownHome: boolean;
  ownPlot: boolean;
  willingToContribute: boolean;
}

export type WizardFormErrors = Partial<Record<keyof WizardFormState, string>>;

export function emptyWizardState(): WizardFormState {
  return {
    headFirstName: '',
    headMiddleName: '',
    headLastName: '',
    familyName: '',
    headGender: '',
    headDateOfBirth: '',
    mobileNumber: '',
    email: '',
    aadhaarNumber: '',
    photo: null,

    address: '',
    country: 'India',
    state: '',
    district: '',
    areaLocality: '',
    pinCode: '',

    samaj: '',
    gotra: '',
    otherGotra: '',
    nativePlace: '',

    occupationBusinessType: '',
    annualIncomeRange: '',
    familyCategory: '',
    ownTwoWheeler: false,
    ownFourWheeler: false,
    ownHome: false,
    ownPlot: false,
    willingToContribute: false,
  };
}

export interface StepProps {
  values: WizardFormState;
  errors: WizardFormErrors;
  setField: <K extends keyof WizardFormState>(key: K, value: WizardFormState[K]) => void;
}

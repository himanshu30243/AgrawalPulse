// Fixed Gotra dropdown list per frontend/docs/family-registration.md's Step 3 "Gotra" field.
// 'Other' triggers a conditional free-text "Other Gotra" input in the form - the typed value
// becomes the submitted gotra string directly (see FamilyRegistrationWizardPage), no separate
// backend field needed for it.
export const GOTRA_OPTIONS = [
  'Airan',
  'Bansal',
  'Bindal',
  'Garg',
  'Goyal',
  'Goenka',
  'Jindal',
  'Kansal',
  'Mittal',
  'Mangal',
  'Singhal',
  'Tayal',
  'Dharan',
  'Kuchhal',
  'Madhukul',
  'Nangal',
  'Tingle',
  'Bhandal',
  'Other',
] as const;

// Static country/state/district reference data for the family registration wizard's Address step
// (frontend/docs/family-registration.md Step 2). India gets PIN-code-driven auto-fill instead of
// this list now (see AddressStep.tsx / familiesApi.lookupPincode) - STATES_BY_COUNTRY/
// DISTRICTS_BY_STATE below only remain as the fallback when that lookup fails or is unreachable.
// Deliberately a pragmatic starting subset, not an exhaustive master-data table (India has 700+
// real districts) - extend this list as chapters actually need districts it's missing.
//
// Non-India entries exist so NRI/diaspora families can actually select their country (previously
// 'India' was the only option in this list at all) - State/City are plain free-text for every
// non-India country rather than another cascading dropdown, since we hold no data for them.
export const COUNTRIES = [
  'India',
  'United States',
  'United Kingdom',
  'Canada',
  'Australia',
  'United Arab Emirates',
  'Singapore',
  'Other',
] as const;

export const STATES_BY_COUNTRY: Record<string, string[]> = {
  India: [
    'Madhya Pradesh',
    'Rajasthan',
    'Maharashtra',
    'Delhi',
    'Uttar Pradesh',
    'Gujarat',
    'Haryana',
    'Punjab',
    'West Bengal',
    'Karnataka',
  ],
};

export const DISTRICTS_BY_STATE: Record<string, string[]> = {
  'Madhya Pradesh': ['Indore', 'Bhopal', 'Ujjain', 'Gwalior', 'Jabalpur', 'Ratlam', 'Dewas'],
  Rajasthan: ['Jaipur', 'Jodhpur', 'Udaipur', 'Kota', 'Ajmer', 'Bikaner'],
  Maharashtra: ['Mumbai', 'Pune', 'Nagpur', 'Nashik', 'Aurangabad'],
  Delhi: ['New Delhi', 'North Delhi', 'South Delhi', 'East Delhi', 'West Delhi'],
  'Uttar Pradesh': ['Lucknow', 'Kanpur', 'Agra', 'Varanasi', 'Meerut'],
  Gujarat: ['Ahmedabad', 'Surat', 'Vadodara', 'Rajkot'],
  Haryana: ['Gurugram', 'Faridabad', 'Panipat', 'Ambala'],
  Punjab: ['Ludhiana', 'Amritsar', 'Jalandhar', 'Patiala'],
  'West Bengal': ['Kolkata', 'Howrah', 'Durgapur', 'Siliguri'],
  Karnataka: ['Bengaluru', 'Mysuru', 'Hubballi', 'Mangaluru'],
};

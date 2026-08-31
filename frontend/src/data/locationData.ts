// Static India-only country/state/district reference data for the family registration wizard's
// cascading Address dropdowns (frontend/docs/family-registration.md Step 2). Deliberately a
// pragmatic starting subset, not an exhaustive master-data table (India has 700+ real districts)
// - no backend master-data service exists or is planned for this; extend this list as chapters
// actually need districts it's missing.
export const COUNTRIES = ['India'] as const;

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

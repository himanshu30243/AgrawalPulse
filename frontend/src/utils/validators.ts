const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const INDIAN_PHONE_RE = /^[6-9]\d{9}$/;
const PIN_CODE_RE = /^\d{6}$/;
const ALPHABETIC_NAME_RE = /^[A-Za-z ]+$/;
const AADHAAR_RE = /^\d{12}$/;

export function isValidEmail(value: string): boolean {
  return EMAIL_RE.test(value);
}

export function isValidIndianPhone(value: string): boolean {
  return INDIAN_PHONE_RE.test(value);
}

export function isValidPinCode(value: string): boolean {
  return PIN_CODE_RE.test(value);
}

export function isAlphabeticName(value: string): boolean {
  return ALPHABETIC_NAME_RE.test(value);
}

export function isValidAadhaar(value: string): boolean {
  return AADHAAR_RE.test(value);
}

export function calculateAge(dateOfBirth: string): number {
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return 0;
  const today = new Date();
  let age = today.getFullYear() - dob.getFullYear();
  const monthDiff = today.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
    age -= 1;
  }
  return age;
}

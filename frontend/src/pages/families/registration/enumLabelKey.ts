// "families.categoryBUSINESS" etc. would collide with i18next's key-casing conventions, so
// option labels are built from a fixed lookup instead of string-transforming the enum value
// directly (same approach FamilyRegistrationForm used before this wizard replaced it).
export function enumLabelKey(prefix: string, value: string): string {
  return `families.${prefix}${value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')}`;
}

// Client-side mirror of backend/membership-service's FinancialYearUtil - India FY (Apr-Mar),
// represented by its start year (e.g. 2026 = "FY 2026-27"). Used only for display defaults
// (which FY to preselect in a filter/form) - the backend is always the authority on any FY value
// actually persisted or used to compute status.
export function currentFinancialYear(date: Date = new Date()): number {
  const month = date.getMonth() + 1; // Date.getMonth() is 0-indexed
  return month >= 4 ? date.getFullYear() : date.getFullYear() - 1;
}

export function financialYearLabel(financialYearStart: number): string {
  const endYear = (financialYearStart + 1) % 100;
  return `${financialYearStart}-${endYear.toString().padStart(2, '0')}`;
}

/** A small window of selectable financial years for filters/forms: a few years back through next. */
export function financialYearOptions(around: number = currentFinancialYear()): number[] {
  const years: number[] = [];
  for (let year = around + 1; year >= around - 4; year -= 1) {
    years.push(year);
  }
  return years;
}

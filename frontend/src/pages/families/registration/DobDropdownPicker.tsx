import { useEffect, useState } from 'react';
import Grid from '@mui/material/Grid2';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Typography from '@mui/material/Typography';
import FormHelperText from '@mui/material/FormHelperText';
import { useTranslation } from 'react-i18next';

interface DobDropdownPickerProps {
  label: string;
  // ISO 'yyyy-MM-dd', or '' when unset - kept as the single source of truth so the parent's form
  // state doesn't need three separate day/month/year fields per date (see
  // frontend/docs/family-registration.md's "User Friendly DOB Picker" - day/month/year selects,
  // no calendar widget, quick year selection for senior citizens filling this in).
  value: string;
  onChange: (isoDate: string) => void;
  error?: string;
  required?: boolean;
}

const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

const CURRENT_YEAR = new Date().getFullYear();
// A century of years is enough range for any living family member/head - descending so the most
// likely-relevant recent decades appear first in the dropdown instead of the 1920s.
const YEARS = Array.from({ length: 100 }, (_, i) => CURRENT_YEAR - i);
const DAYS = Array.from({ length: 31 }, (_, i) => i + 1);

function parseIso(value: string): { day: string; month: string; year: string } {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return { day: '', month: '', year: '' };
  // Capture groups are guaranteed present whenever the regex matches at all (fixed 3-group
  // pattern) - noUncheckedIndexedAccess just can't infer that from a generic array type.
  const year = match[1] as string;
  const month = match[2] as string;
  const day = match[3] as string;
  return { day: String(Number(day)), month: String(Number(month) - 1), year };
}

function toIso(day: string, month: string, year: string): string {
  if (!day || !month || !year) return '';
  const d = day.padStart(2, '0');
  const m = String(Number(month) + 1).padStart(2, '0');
  return `${year}-${m}-${d}`;
}

export function DobDropdownPicker({ label, value, onChange, error, required }: DobDropdownPickerProps) {
  const { t } = useTranslation();
  // Local state, not fully derived from `value` on every render: a partial selection (e.g. just
  // Day, with Month/Year still unset) can't form a valid ISO date, so it has nothing to emit
  // upward yet - if this component instead re-derived day/month/year from `value` each render,
  // that partial progress would be silently discarded (parent's headDateOfBirth stays '', so
  // parseIso('') would reset all three selects back to blank on the next render, and the picker
  // could never actually be completed one field at a time).
  const [day, setDay] = useState(() => parseIso(value).day);
  const [month, setMonth] = useState(() => parseIso(value).month);
  const [year, setYear] = useState(() => parseIso(value).year);

  useEffect(() => {
    // Re-sync from an externally-set value (e.g. a restored draft) - but not from our own
    // emissions, which always originate from this component's own local state already matching.
    const parsed = parseIso(value);
    setDay(parsed.day);
    setMonth(parsed.month);
    setYear(parsed.year);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  const emit = (nextDay: string, nextMonth: string, nextYear: string) => {
    setDay(nextDay);
    setMonth(nextMonth);
    setYear(nextYear);
    const iso = toIso(nextDay, nextMonth, nextYear);
    if (!iso) return;
    // A future date is invalid for a date of birth - simply don't emit it rather than letting an
    // impossible value reach the parent's form state.
    if (iso > new Date().toISOString().slice(0, 10)) return;
    onChange(iso);
  };

  return (
    <Grid container spacing={1}>
      <Grid size={12}>
        <Typography variant="body2" color={error ? 'error' : 'text.secondary'}>
          {label}
          {required ? ' *' : ''}
        </Typography>
      </Grid>
      <Grid size={4}>
        <TextField
          select
          label={t('families.dobDay')}
          value={day}
          onChange={(e) => emit(e.target.value, month, year)}
          fullWidth
          size="small"
          error={Boolean(error)}
        >
          {DAYS.map((d) => (
            <MenuItem key={d} value={String(d)}>
              {String(d).padStart(2, '0')}
            </MenuItem>
          ))}
        </TextField>
      </Grid>
      <Grid size={4}>
        <TextField
          select
          label={t('families.dobMonth')}
          value={month}
          onChange={(e) => emit(day, e.target.value, year)}
          fullWidth
          size="small"
          error={Boolean(error)}
        >
          {MONTHS.map((m, i) => (
            <MenuItem key={m} value={String(i)}>
              {m}
            </MenuItem>
          ))}
        </TextField>
      </Grid>
      <Grid size={4}>
        <TextField
          select
          label={t('families.dobYear')}
          value={year}
          onChange={(e) => emit(day, month, e.target.value)}
          fullWidth
          size="small"
          error={Boolean(error)}
        >
          {YEARS.map((y) => (
            <MenuItem key={y} value={String(y)}>
              {y}
            </MenuItem>
          ))}
        </TextField>
      </Grid>
      {error && (
        <Grid size={12}>
          <FormHelperText error>{error}</FormHelperText>
        </Grid>
      )}
    </Grid>
  );
}

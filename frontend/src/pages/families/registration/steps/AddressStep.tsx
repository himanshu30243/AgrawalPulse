import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid2';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import { familiesApi } from '@/api/familiesApi';
import { COUNTRIES, DISTRICTS_BY_STATE, STATES_BY_COUNTRY } from '@/data/locationData';
import type { StepProps } from '../types';

type PincodeLookupStatus = 'idle' | 'loading' | 'success' | 'error';

function StepLabel({ text, required }: { text: string; required?: boolean }) {
  return (
    <Typography
      component="label"
      variant="caption"
      sx={{
        display: 'block',
        fontWeight: 600,
        fontSize: '0.7rem',
        textTransform: 'uppercase',
        letterSpacing: '0.05em',
        color: 'text.secondary',
        mb: 1,
      }}
    >
      {text}
      {required && <span style={{ color: '#C0392B', marginLeft: '2px' }}>*</span>}
    </Typography>
  );
}

function SectionHeader({ text }: { text: string }) {
  return (
    <Box sx={{
      mb: 3,
      pb: 1.5,
      borderBottom: '1px solid rgba(124, 29, 29, 0.15)',
    }}>
      <Typography
        variant="caption"
        sx={{
          display: 'block',
          fontWeight: 600,
          fontSize: '0.75rem',
          textTransform: 'uppercase',
          letterSpacing: '0.08em',
          color: 'primary.main',
        }}
      >
        {text}
      </Typography>
    </Box>
  );
}

export function AddressStep({ values, errors, setField }: StepProps) {
  const { t } = useTranslation();
  const isIndia = values.country === 'India';
  const states = STATES_BY_COUNTRY[values.country] ?? [];
  const districts = DISTRICTS_BY_STATE[values.state] ?? [];
  const [pincodeStatus, setPincodeStatus] = useState<PincodeLookupStatus>('idle');

  // Auto-fills State/District from the PIN code (family-service proxies India Post's public PIN
  // lookup - see familiesApi.lookupPincode) once it's a complete 6-digit number. `setField` is
  // deliberately excluded from the deps array - it's a new function identity on every keystroke
  // anywhere in the wizard (not just this field), and including it would re-run this lookup on
  // every unrelated field edit rather than only when the PIN code itself changes.
  useEffect(() => {
    if (!isIndia || values.pinCode.length !== 6) {
      setPincodeStatus('idle');
      return;
    }
    let cancelled = false;
    setPincodeStatus('loading');
    familiesApi.lookupPincode(values.pinCode).then((result) => {
      if (cancelled) return;
      if (result) {
        setField('state', result.state);
        setField('district', result.district);
        setPincodeStatus('success');
      } else {
        // Invalid PIN or the lookup service is unreachable - fall back to the manual
        // State/District dropdowns below rather than blocking registration on it.
        setPincodeStatus('error');
      }
    });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [values.pinCode, isIndia]);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <SectionHeader text={t('families.residentialAddress') || 'Residential Address'} />
      <Grid container spacing={2}>
      <Grid size={12}>
        <StepLabel text={t('families.address')} required />
        <TextField
          placeholder="House No., Street Name, Colony / Society…"
          value={values.address}
          onChange={(e) => setField('address', e.target.value)}
          error={Boolean(errors.address)}
          helperText={errors.address}
          fullWidth
          multiline
          minRows={2}
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#FFFFFF',
              borderRadius: '12px',
            },
          }}
        />
      </Grid>
      <Grid size={{ xs: 12, sm: 4 }}>
        <StepLabel text={t('families.country')} required />
        <TextField
          select
          placeholder="Select country"
          value={values.country}
          onChange={(e) => {
            setField('country', e.target.value);
            setField('state', '');
            setField('district', '');
            // A 6-digit Indian PIN and a free-text foreign postal code aren't interchangeable -
            // clear it rather than carry a value across that no longer means the same thing.
            setField('pinCode', '');
          }}
          error={Boolean(errors.country)}
          helperText={errors.country}
          fullWidth
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#FFFFFF',
              borderRadius: '12px',
            },
          }}
        >
          {COUNTRIES.map((country) => (
            <MenuItem key={country} value={country}>
              {country}
            </MenuItem>
          ))}
        </TextField>
      </Grid>
      {isIndia && (
        <Grid size={{ xs: 12, sm: 4 }}>
          <StepLabel text={t('families.pinCode')} required />
          <TextField
            placeholder="302001"
            value={values.pinCode}
            onChange={(e) => setField('pinCode', e.target.value.replace(/\D/g, '').slice(0, 6))}
            error={Boolean(errors.pinCode)}
            helperText={
              errors.pinCode ??
              (pincodeStatus === 'loading'
                ? 'Looking up…'
                : pincodeStatus === 'success'
                  ? 'State and District filled in automatically - edit them if needed'
                  : pincodeStatus === 'error'
                    ? 'Could not look up this PIN code - please select State/District below'
                    : undefined)
            }
            fullWidth
            size="small"
            inputMode="numeric"
            InputProps={{
              endAdornment: pincodeStatus === 'loading' ? <CircularProgress size={16} /> : undefined,
            }}
            sx={{
              '& .MuiOutlinedInput-root': {
                bgcolor: '#FFFFFF',
                borderRadius: '12px',
              },
            }}
          />
        </Grid>
      )}
      {isIndia && pincodeStatus === 'success' ? (
        <>
          <Grid size={{ xs: 12, sm: 4 }}>
            <StepLabel text={t('families.state')} required />
            <TextField
              value={values.state}
              onChange={(e) => setField('state', e.target.value)}
              error={Boolean(errors.state)}
              helperText={errors.state}
              fullWidth
              size="small"
              sx={{
                '& .MuiOutlinedInput-root': {
                  bgcolor: '#FFFFFF',
                  borderRadius: '12px',
                },
              }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <StepLabel text={t('families.district')} required />
            <TextField
              value={values.district}
              onChange={(e) => setField('district', e.target.value)}
              error={Boolean(errors.district)}
              helperText={errors.district}
              fullWidth
              size="small"
              sx={{
                '& .MuiOutlinedInput-root': {
                  bgcolor: '#FFFFFF',
                  borderRadius: '12px',
                },
              }}
            />
          </Grid>
        </>
      ) : isIndia ? (
        <>
          <Grid size={{ xs: 12, sm: 4 }}>
            <StepLabel text={t('families.state')} required />
            <TextField
              select
              placeholder={values.country ? 'Select state' : 'Select country first'}
              value={values.state}
              onChange={(e) => {
                setField('state', e.target.value);
                setField('district', '');
              }}
              error={Boolean(errors.state)}
              helperText={errors.state}
              fullWidth
              size="small"
              disabled={states.length === 0}
              sx={{
                '& .MuiOutlinedInput-root': {
                  bgcolor: '#FFFFFF',
                  borderRadius: '12px',
                },
              }}
            >
              {states.map((state) => (
                <MenuItem key={state} value={state}>
                  {state}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <StepLabel text={t('families.district')} required />
            <TextField
              select
              placeholder={values.state ? 'Select district' : 'Select state first'}
              value={values.district}
              onChange={(e) => setField('district', e.target.value)}
              error={Boolean(errors.district)}
              helperText={errors.district}
              fullWidth
              size="small"
              disabled={districts.length === 0}
              sx={{
                '& .MuiOutlinedInput-root': {
                  bgcolor: '#FFFFFF',
                  borderRadius: '12px',
                },
              }}
            >
              {districts.map((district) => (
                <MenuItem key={district} value={district}>
                  {district}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
        </>
      ) : (
        // No reference data for any non-India country - plain free text rather than a dropdown
        // stuck permanently empty/disabled.
        <>
          <Grid size={{ xs: 12, sm: 6 }}>
            <StepLabel text={t('families.state')} required />
            <TextField
              placeholder="State / Province"
              value={values.state}
              onChange={(e) => setField('state', e.target.value)}
              error={Boolean(errors.state)}
              helperText={errors.state}
              fullWidth
              size="small"
              sx={{
                '& .MuiOutlinedInput-root': {
                  bgcolor: '#FFFFFF',
                  borderRadius: '12px',
                },
              }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <StepLabel text={t('families.district')} required />
            <TextField
              placeholder="City"
              value={values.district}
              onChange={(e) => setField('district', e.target.value)}
              error={Boolean(errors.district)}
              helperText={errors.district}
              fullWidth
              size="small"
              sx={{
                '& .MuiOutlinedInput-root': {
                  bgcolor: '#FFFFFF',
                  borderRadius: '12px',
                },
              }}
            />
          </Grid>
        </>
      )}
      <Grid size={{ xs: 12, sm: isIndia ? 6 : 12 }}>
        <StepLabel text={t('families.areaLocality')} required />
        <TextField
          placeholder="e.g., Vaishali Nagar, Sector 12"
          value={values.areaLocality}
          onChange={(e) => setField('areaLocality', e.target.value)}
          error={Boolean(errors.areaLocality)}
          helperText={errors.areaLocality}
          fullWidth
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#FFFFFF',
              borderRadius: '12px',
            },
          }}
        />
      </Grid>
      {!isIndia && (
        <Grid size={{ xs: 12, sm: 6 }}>
          <StepLabel text={t('families.pinCode')} required />
          <TextField
            placeholder="Postal code"
            value={values.pinCode}
            onChange={(e) => setField('pinCode', e.target.value)}
            error={Boolean(errors.pinCode)}
            helperText={errors.pinCode}
            fullWidth
            size="small"
            sx={{
              '& .MuiOutlinedInput-root': {
                bgcolor: '#FFFFFF',
                borderRadius: '12px',
              },
            }}
          />
        </Grid>
      )}
    </Grid>
    </Box>
  );
}

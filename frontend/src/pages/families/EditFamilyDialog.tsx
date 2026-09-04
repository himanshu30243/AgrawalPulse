import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import axios from 'axios';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Stack from '@mui/material/Stack';
import Grid from '@mui/material/Grid2';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import CircularProgress from '@mui/material/CircularProgress';
import { familiesApi } from '@/api/familiesApi';
import { COUNTRIES } from '@/data/locationData';
import { isAlphabeticName, isValidIndianPhone } from '@/utils/validators';
import type { Family } from '@/types/domain';

interface Props {
  family: Family;
  onClose: () => void;
  onSaved: (updated: Family) => void;
}

type PincodeLookupStatus = 'idle' | 'loading' | 'success' | 'error';

interface FormErrors {
  headFirstName?: string;
  headLastName?: string;
  mobileNumber?: string;
  country?: string;
  state?: string;
  district?: string;
}

// Head name/contact/location only - matches UpdateFamilyRequest exactly (see its comment for why
// the rest of the family's fields have no edit path yet). Location reuses the same PIN-code
// auto-fill UX as the registration wizard's AddressStep, since editing your city is expected to
// behave the same way entering it did the first time - see familiesApi.lookupPincode.
export function EditFamilyDialog({ family, onClose, onSaved }: Props) {
  const { t } = useTranslation();

  const [headFirstName, setHeadFirstName] = useState(family.headFirstName);
  const [headMiddleName, setHeadMiddleName] = useState(family.headMiddleName ?? '');
  const [headLastName, setHeadLastName] = useState(family.headLastName);
  const [mobileNumber, setMobileNumber] = useState(family.mobileNumber);
  const [email, setEmail] = useState(family.email);

  const [country, setCountry] = useState(family.country || 'India');
  const [pinCode, setPinCode] = useState('');
  const [state, setState] = useState(family.state);
  const [district, setDistrict] = useState(family.district);
  const [pincodeStatus, setPincodeStatus] = useState<PincodeLookupStatus>('idle');
  const isIndia = country === 'India';

  const [errors, setErrors] = useState<FormErrors>({});
  const [isSaving, setIsSaving] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Same pattern as AddressStep/RegistrationPage: auto-fill State/City once the PIN code is a
  // complete 6-digit number. Deliberately does not touch the already-loaded state/district until
  // the caller actually types a new PIN - editing the dialog shouldn't blank out fields nobody
  // asked to change yet.
  useEffect(() => {
    if (!isIndia || pinCode.length !== 6) {
      setPincodeStatus('idle');
      return;
    }
    let cancelled = false;
    setPincodeStatus('loading');
    familiesApi.lookupPincode(pinCode).then((result) => {
      if (cancelled) return;
      if (result) {
        setState(result.state);
        setDistrict(result.district);
        setPincodeStatus('success');
      } else {
        setPincodeStatus('error');
      }
    });
    return () => {
      cancelled = true;
    };
  }, [pinCode, isIndia]);

  const handleSave = async () => {
    const nextErrors: FormErrors = {};
    if (!headFirstName.trim() || !isAlphabeticName(headFirstName)) {
      nextErrors.headFirstName = t('families.alphabetsOnly');
    }
    if (!headLastName.trim() || !isAlphabeticName(headLastName)) {
      nextErrors.headLastName = t('families.alphabetsOnly');
    }
    if (!isValidIndianPhone(mobileNumber)) {
      nextErrors.mobileNumber = t('families.invalidMobile');
    }
    if (!country) nextErrors.country = t('common.required');
    if (!state.trim()) nextErrors.state = t('common.required');
    if (!district.trim()) nextErrors.district = t('common.required');
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setIsSaving(true);
    setSubmitError(null);
    try {
      const updated = await familiesApi.update(family.id, {
        headFirstName: headFirstName.trim(),
        headMiddleName: headMiddleName.trim(),
        headLastName: headLastName.trim(),
        mobileNumber: mobileNumber.trim(),
        email: email.trim(),
        country,
        state,
        district,
      });
      onSaved(updated);
    } catch (error) {
      const backendMessage =
        axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
          ? error.response.data.message
          : null;
      setSubmitError(backendMessage ?? t('families.updateFamilyError'));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{t('families.editFamily')}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2.5} sx={{ mt: 0.5 }}>
          {submitError && <Alert severity="error">{submitError}</Alert>}

          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                label={t('families.headFirstName')}
                value={headFirstName}
                onChange={(e) => setHeadFirstName(e.target.value)}
                error={Boolean(errors.headFirstName)}
                helperText={errors.headFirstName}
                size="small"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                label={t('families.headMiddleName')}
                value={headMiddleName}
                onChange={(e) => setHeadMiddleName(e.target.value)}
                size="small"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                label={t('families.headLastName')}
                value={headLastName}
                onChange={(e) => setHeadLastName(e.target.value)}
                error={Boolean(errors.headLastName)}
                helperText={errors.headLastName}
                size="small"
              />
            </Grid>
          </Grid>

          <TextField
            fullWidth
            label={t('families.mobileNumber')}
            value={mobileNumber}
            onChange={(e) => setMobileNumber(e.target.value.replace(/\D/g, '').slice(0, 10))}
            error={Boolean(errors.mobileNumber)}
            helperText={errors.mobileNumber}
            size="small"
            inputMode="numeric"
          />

          <TextField
            fullWidth
            type="email"
            label={t('families.email')}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            size="small"
          />

          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: isIndia ? 4 : 12 }}>
              <TextField
                select
                fullWidth
                label={t('families.country')}
                value={country}
                onChange={(e) => {
                  setCountry(e.target.value);
                  setState('');
                  setDistrict('');
                  setPinCode('');
                }}
                error={Boolean(errors.country)}
                helperText={errors.country}
                size="small"
              >
                {COUNTRIES.map((c) => (
                  <MenuItem key={c} value={c}>
                    {c}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            {isIndia && (
              <Grid size={{ xs: 12, sm: 8 }}>
                <TextField
                  fullWidth
                  label={t('families.pinCode')}
                  placeholder="452001"
                  value={pinCode}
                  onChange={(e) => setPinCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  helperText={
                    pincodeStatus === 'loading'
                      ? t('families.pincodeLookupLoading')
                      : pincodeStatus === 'success'
                        ? t('families.pincodeLookupSuccess')
                        : pincodeStatus === 'error'
                          ? t('families.pincodeLookupError')
                          : undefined
                  }
                  size="small"
                  inputMode="numeric"
                  InputProps={{
                    endAdornment: pincodeStatus === 'loading' ? <CircularProgress size={16} /> : undefined,
                  }}
                />
              </Grid>
            )}
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                label={isIndia ? t('families.state') : `${t('families.state')} / Province`}
                value={state}
                onChange={(e) => setState(e.target.value)}
                error={Boolean(errors.state)}
                helperText={errors.state}
                size="small"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                label={t('families.city')}
                value={district}
                onChange={(e) => setDistrict(e.target.value)}
                error={Boolean(errors.district)}
                helperText={errors.district}
                size="small"
              />
            </Grid>
          </Grid>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common.cancel')}</Button>
        <Button variant="contained" disabled={isSaving} onClick={() => void handleSave()}>
          {isSaving ? t('common.loading') : t('common.save')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid2';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormControl from '@mui/material/FormControl';
import FormLabel from '@mui/material/FormLabel';
import Radio from '@mui/material/Radio';
import RadioGroup from '@mui/material/RadioGroup';
import Alert from '@mui/material/Alert';
import Typography from '@mui/material/Typography';
import InfoIcon from '@mui/icons-material/Info';
import { enumLabelKey } from '../enumLabelKey';
import type { StepProps, WizardFormState } from '../types';
import type { FamilyCategory } from '@/types/domain';

const FAMILY_CATEGORIES: FamilyCategory[] = ['BUSINESS', 'SALARIED', 'PROFESSIONAL', 'RETIRED', 'AGRICULTURE', 'OTHER'];
const ANNUAL_INCOME_RANGES = [
  'Below ₹5 Lakh',
  '₹5-10 Lakh',
  '₹10-25 Lakh',
  '₹25-50 Lakh',
  '₹50 Lakh-1 Crore',
  'Above ₹1 Crore',
];

type YesNoField = 'ownTwoWheeler' | 'ownFourWheeler' | 'ownHome' | 'ownPlot' | 'willingToContribute';

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

function YesNoRadioGroup({
  label,
  field,
  values,
  setField,
}: {
  label: string;
  field: YesNoField;
  values: WizardFormState;
  setField: StepProps['setField'];
}) {
  const { t } = useTranslation();
  return (
    <FormControl sx={{ width: '100%' }}>
      <FormLabel sx={{ fontWeight: 600, fontSize: '0.9rem', mb: 1.5, color: 'text.primary' }}>
        {label}
      </FormLabel>
      <RadioGroup
        row
        value={values[field] ? 'yes' : 'no'}
        onChange={(e) => setField(field, e.target.value === 'yes')}
        sx={{ gap: 3 }}
      >
        <FormControlLabel
          value="yes"
          control={
            <Radio
              sx={{
                color: 'text.secondary',
                '&.Mui-checked': {
                  color: 'primary.main',
                },
              }}
            />
          }
          label={t('common.yes')}
        />
        <FormControlLabel
          value="no"
          control={
            <Radio
              sx={{
                color: 'text.secondary',
                '&.Mui-checked': {
                  color: 'primary.main',
                },
              }}
            />
          }
          label={t('common.no')}
        />
      </RadioGroup>
    </FormControl>
  );
}

export function FinancialStep({ values, errors, setField }: StepProps) {
  const { t } = useTranslation();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <Alert
        severity="info"
        icon={<InfoIcon sx={{ fontSize: 20 }} />}
        sx={{
          bgcolor: 'rgba(192, 91, 11, 0.08)',
          border: '1px solid rgba(192, 91, 11, 0.2)',
          color: 'text.primary',
          '& .MuiAlert-message': {
            width: '100%',
          },
        }}
      >
        <Typography variant="body2">
          <strong>{t('common.optional') || 'Optional'}</strong> — {t('families.financialOptionalNote') || 'These details help us serve the community better — share only what you are comfortable with.'}
        </Typography>
      </Alert>

      <SectionHeader text={t('families.financialSocialInfo') || 'Financial & Social Information'} />

      <Grid container spacing={2}>
      <Grid size={{ xs: 12, sm: 6 }}>
        <StepLabel text={t('families.occupationBusinessType')} />
        <TextField
          placeholder="e.g., Business Owner, Doctor, CA"
          value={values.occupationBusinessType}
          onChange={(e) => setField('occupationBusinessType', e.target.value)}
          fullWidth
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#FFFFFF',
              borderRadius: '12px',
            },
          }}
        />
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
          {t('families.occupationHelper') || 'Describe your primary occupation or business'}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, sm: 6 }}>
        <StepLabel text={t('families.annualIncomeRange')} />
        <TextField
          select
          placeholder="Select range"
          value={values.annualIncomeRange}
          onChange={(e) => setField('annualIncomeRange', e.target.value)}
          fullWidth
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#FFFFFF',
              borderRadius: '12px',
            },
          }}
        >
          <MenuItem value="">{t('common.notSpecified')}</MenuItem>
          {ANNUAL_INCOME_RANGES.map((range) => (
            <MenuItem key={range} value={range}>
              {range}
            </MenuItem>
          ))}
        </TextField>
      </Grid>
      <Grid size={{ xs: 12, sm: 6 }}>
        <StepLabel text={t('families.familyCategory')} />
        <TextField
          select
          placeholder="Select category"
          value={values.familyCategory}
          onChange={(e) => setField('familyCategory', e.target.value as FamilyCategory)}
          error={Boolean(errors.familyCategory)}
          helperText={errors.familyCategory}
          fullWidth
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#FFFFFF',
              borderRadius: '12px',
            },
          }}
        >
          <MenuItem value="">{t('common.notSpecified')}</MenuItem>
          {FAMILY_CATEGORIES.map((category) => (
            <MenuItem key={category} value={category}>
              {t(enumLabelKey('category', category))}
            </MenuItem>
          ))}
        </TextField>
      </Grid>

      <Grid size={{ xs: 12, sm: 6 }}>
        <YesNoRadioGroup label={t('families.ownTwoWheeler')} field="ownTwoWheeler" values={values} setField={setField} />
      </Grid>
      <Grid size={{ xs: 12, sm: 6 }}>
        <YesNoRadioGroup label={t('families.ownFourWheeler')} field="ownFourWheeler" values={values} setField={setField} />
      </Grid>
      <Grid size={{ xs: 12, sm: 6 }}>
        <YesNoRadioGroup label={t('families.ownHome')} field="ownHome" values={values} setField={setField} />
      </Grid>
      <Grid size={{ xs: 12, sm: 6 }}>
        <YesNoRadioGroup label={t('families.ownPlot')} field="ownPlot" values={values} setField={setField} />
      </Grid>
      <Grid size={12}>
        <YesNoRadioGroup
          label={t('families.willingToContribute')}
          field="willingToContribute"
          values={values}
          setField={setField}
        />
      </Grid>
    </Grid>
    </Box>
  );
}

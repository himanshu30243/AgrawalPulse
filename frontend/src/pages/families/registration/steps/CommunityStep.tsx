import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid2';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Typography from '@mui/material/Typography';
import { GOTRA_OPTIONS } from '@/data/gotraOptions';
import { enumLabelKey } from '../enumLabelKey';
import type { StepProps } from '../types';
import type { Samaj } from '@/types/domain';

const SAMAJ_OPTIONS: Samaj[] = ['AGRAWAL', 'OTHER'];

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

export function CommunityStep({ values, errors, setField }: StepProps) {
  const { t } = useTranslation();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <SectionHeader text={t('families.communityBackground') || 'Community & Cultural Background'} />
      <Grid container spacing={2}>
      <Grid size={{ xs: 12, sm: 6 }}>
        <StepLabel text={t('families.samaj')} required />
        <TextField
          select
          placeholder="Select Samaj"
          value={values.samaj}
          onChange={(e) => setField('samaj', e.target.value as Samaj)}
          error={Boolean(errors.samaj)}
          helperText={errors.samaj}
          fullWidth
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#FFFFFF',
              borderRadius: '12px',
            },
          }}
        >
          {SAMAJ_OPTIONS.map((samaj) => (
            <MenuItem key={samaj} value={samaj}>
              {t(enumLabelKey('samaj', samaj))}
            </MenuItem>
          ))}
        </TextField>
      </Grid>
      <Grid size={{ xs: 12, sm: 6 }}>
        <StepLabel text={t('families.gotra')} required />
        <TextField
          select
          placeholder="Select Gotra"
          value={values.gotra}
          onChange={(e) => setField('gotra', e.target.value)}
          error={Boolean(errors.gotra)}
          helperText={errors.gotra}
          fullWidth
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#FFFFFF',
              borderRadius: '12px',
            },
          }}
        >
          {GOTRA_OPTIONS.map((gotra) => (
            <MenuItem key={gotra} value={gotra}>
              {gotra === 'Other' ? t('families.gotraOther') : gotra}
            </MenuItem>
          ))}
        </TextField>
      </Grid>
      {values.gotra === 'Other' && (
        <Grid size={{ xs: 12, sm: 6 }}>
          <StepLabel text={t('families.otherGotra')} required />
          <TextField
            placeholder="Enter your Gotra"
            value={values.otherGotra}
            onChange={(e) => setField('otherGotra', e.target.value)}
            error={Boolean(errors.otherGotra)}
            helperText={errors.otherGotra}
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
      <Grid size={{ xs: 12, sm: 6 }}>
        <StepLabel text={t('families.nativePlace')} required />
        <TextField
          placeholder="e.g., Agroha, Haryana"
          value={values.nativePlace}
          onChange={(e) => setField('nativePlace', e.target.value)}
          error={Boolean(errors.nativePlace)}
          helperText={errors.nativePlace}
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
        <StepLabel text={t('families.regionCity')} />
        <TextField
          placeholder="Auto-populated from district"
          value={values.district}
          fullWidth
          disabled
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#F0E8D8',
              borderRadius: '12px',
            },
          }}
        />
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
          {t('families.regionCityHelper')}
        </Typography>
      </Grid>
    </Grid>
    </Box>
  );
}

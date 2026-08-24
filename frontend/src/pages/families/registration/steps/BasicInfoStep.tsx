import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid2';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Avatar from '@mui/material/Avatar';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { DobDropdownPicker } from '../DobDropdownPicker';
import { enumLabelKey } from '../enumLabelKey';
import type { StepProps } from '../types';
import type { Gender } from '@/types/domain';

const GENDERS: Gender[] = ['MALE', 'FEMALE', 'OTHER'];

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

export function BasicInfoStep({ values, errors, setField }: StepProps) {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <SectionHeader text={t('families.headFamilyDetails') || 'Head of Family Details'} />

      <Grid container spacing={2}>
      <Grid size={{ xs: 12, sm: 4 }}>
        <StepLabel text={t('families.headFirstName')} required />
        <TextField
          placeholder="Rajesh"
          value={values.headFirstName}
          onChange={(e) => setField('headFirstName', e.target.value)}
          error={Boolean(errors.headFirstName)}
          helperText={errors.headFirstName}
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
        <StepLabel text={t('families.headMiddleName')} />
        <TextField
          placeholder="Kumar"
          value={values.headMiddleName}
          onChange={(e) => setField('headMiddleName', e.target.value)}
          error={Boolean(errors.headMiddleName)}
          helperText={errors.headMiddleName}
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
        <StepLabel text={t('families.headLastName')} required />
        <TextField
          placeholder="Agrawal"
          value={values.headLastName}
          onChange={(e) => setField('headLastName', e.target.value)}
          error={Boolean(errors.headLastName)}
          helperText={errors.headLastName}
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
        <StepLabel text={t('families.headGender')} required />
        <TextField
          select
          placeholder="Select gender"
          value={values.headGender}
          onChange={(e) => setField('headGender', e.target.value as Gender)}
          error={Boolean(errors.headGender)}
          helperText={errors.headGender}
          fullWidth
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#FFFFFF',
              borderRadius: '12px',
            },
          }}
        >
          {GENDERS.map((gender) => (
            <MenuItem key={gender} value={gender}>
              {t(enumLabelKey('gender', gender))}
            </MenuItem>
          ))}
        </TextField>
      </Grid>
      <Grid size={{ xs: 12, sm: 6 }}>
        <StepLabel text={t('families.headDateOfBirth')} required />
        <DobDropdownPicker
          label=""
          value={values.headDateOfBirth}
          onChange={(iso) => setField('headDateOfBirth', iso)}
          error={errors.headDateOfBirth}
          required
        />
      </Grid>

      <Grid size={{ xs: 12, sm: 4 }}>
        <StepLabel text={t('families.mobileNumber')} required />
        <Stack direction="row" gap={1} alignItems="flex-start">
          <Box sx={{
            px: 1.5,
            py: 1,
            bgcolor: '#F0E8D8',
            border: '1px solid rgba(124, 29, 29, 0.15)',
            borderRadius: '12px',
            minWidth: '48px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '0.9rem',
            fontWeight: 500,
            color: 'text.secondary',
            mt: 0.25,
          }}>
            +91
          </Box>
          <TextField
            placeholder="9876543210"
            value={values.mobileNumber}
            onChange={(e) => setField('mobileNumber', e.target.value.replace(/\D/g, '').slice(0, 10))}
            error={Boolean(errors.mobileNumber)}
            helperText={errors.mobileNumber}
            fullWidth
            size="small"
            inputMode="numeric"
            sx={{
              '& .MuiOutlinedInput-root': {
                bgcolor: '#FFFFFF',
                borderRadius: '12px',
              },
            }}
          />
        </Stack>
      </Grid>
      <Grid size={{ xs: 12, sm: 4 }}>
        <StepLabel text={t('families.email')} />
        <TextField
          type="email"
          placeholder="rajesh.agrawal@gmail.com"
          value={values.email}
          onChange={(e) => setField('email', e.target.value)}
          error={Boolean(errors.email)}
          helperText={errors.email}
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
        <StepLabel text={t('families.aadhaarNumber')} />
        <TextField
          placeholder="xxxx xxxx xxxx"
          value={values.aadhaarNumber}
          onChange={(e) => setField('aadhaarNumber', e.target.value.replace(/\D/g, '').slice(0, 12))}
          error={Boolean(errors.aadhaarNumber)}
          helperText={errors.aadhaarNumber}
          fullWidth
          size="small"
          inputMode="numeric"
          sx={{
            '& .MuiOutlinedInput-root': {
              bgcolor: '#FFFFFF',
              borderRadius: '12px',
            },
          }}
        />
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
          {t('families.aadhaarHelper') || '12-digit Aadhaar number (optional)'}
        </Typography>
      </Grid>

      <Grid size={12}>
        <StepLabel text={t('families.profilePhoto')} />
        {values.photo ? (
          <Stack direction="row" gap={2} alignItems="center" sx={{
            p: 2,
            bgcolor: '#FFFFFF',
            border: '1px solid rgba(124, 29, 29, 0.15)',
            borderRadius: '12px',
          }}>
            <Avatar
              src={URL.createObjectURL(values.photo)}
              sx={{ width: 56, height: 56, border: '2px solid rgba(124, 29, 29, 0.2)' }}
            />
            <Stack flex={1} spacing={0.5}>
              <Typography variant="body2" fontWeight={500}>
                {values.photo.name}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {(values.photo.size / 1024).toFixed(0)} KB
              </Typography>
            </Stack>
            <Button
              size="small"
              color="error"
              onClick={() => setField('photo', null)}
              sx={{ textTransform: 'none' }}
            >
              {t('common.remove') || 'Remove'}
            </Button>
          </Stack>
        ) : (
          <Button
            variant="outlined"
            fullWidth
            startIcon={<UploadFileIcon />}
            onClick={() => fileInputRef.current?.click()}
            sx={{
              py: 2,
              border: '2px dashed rgba(124, 29, 29, 0.3)',
              textTransform: 'none',
              color: 'text.secondary',
              '&:hover': {
                borderColor: 'primary.main',
                color: 'primary.main',
                bgcolor: 'rgba(124, 29, 29, 0.02)',
              },
            }}
          >
            {t('families.uploadPhoto') || 'Upload photo (JPG, PNG · max 2 MB)'}
          </Button>
        )}
        {errors.photo && (
          <Typography variant="caption" color="error" sx={{ display: 'block', mt: 1 }}>
            {errors.photo}
          </Typography>
        )}
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png"
          hidden
          onChange={(e) => setField('photo', e.target.files?.[0] ?? null)}
        />
      </Grid>
    </Grid>
    </Box>
  );
}

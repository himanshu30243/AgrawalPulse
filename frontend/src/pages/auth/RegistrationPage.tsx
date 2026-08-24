import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { CheckCircle } from '@mui/icons-material';
import { registrationApi } from '@/api/registrationApi';
import type { Gender } from '@/types/domain';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import Typography from '@mui/material/Typography';
import Link from '@mui/material/Link';
import InputAdornment from '@mui/material/InputAdornment';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Grid from '@mui/material/Grid2';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';
import Divider from '@mui/material/Divider';

// ─── Constants ────────────────────────────────────────────────────────────

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

const DAYS = Array.from({ length: 31 }, (_, i) => String(i + 1).padStart(2, '0'));
const CURRENT_YEAR = new Date().getFullYear();
const YEARS = Array.from({ length: 101 }, (_, i) => String(CURRENT_YEAR - i));

// Backend (com.agrawalpulse.common.model.Gender) only has three values - "Prefer Not To Say" was
// dropped rather than lossily mapped onto one of them.
const GENDERS: { label: string; value: Gender }[] = [
  { label: 'Male', value: 'MALE' },
  { label: 'Female', value: 'FEMALE' },
  { label: 'Other', value: 'OTHER' },
];

// ─── Types ──────────────────────────────────────────────────────────────

interface RegistrationFormState {
  firstName: string;
  middleName: string;
  lastName: string;
  day: string;
  month: string;
  year: string;
  gender: Gender | '';
  mobile: string;
  email: string;
  password: string;
  confirmPassword: string;
  terms: boolean;
}

type RegistrationErrors = Partial<Record<keyof RegistrationFormState | 'dob', string>>;

// ─── Password Strength ────────────────────────────────────────────────────

interface PasswordStrength {
  score: 0 | 1 | 2 | 3;
  label: string;
  color: string;
}

function getPasswordStrength(password: string): PasswordStrength {
  if (!password) return { score: 0, label: '', color: '' };

  let score = 0;
  if (password.length >= 8) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[a-z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;

  if (score <= 2) return { score: 1, label: 'Weak', color: '#FF4D4F' };
  if (score <= 3) return { score: 2, label: 'Medium', color: '#FA8C16' };
  return { score: 3, label: 'Strong', color: '#42B72A' };
}

// ─── Validation ────────────────────────────────────────────────────────────

function validateRegistrationForm(form: RegistrationFormState): RegistrationErrors {
  const errors: RegistrationErrors = {};
  const alphaRegex = /^[a-zA-Z\s]+$/;

  // First Name
  if (!form.firstName.trim()) {
    errors.firstName = 'First name is required';
  } else if (!alphaRegex.test(form.firstName)) {
    errors.firstName = 'Alphabets only';
  } else if (form.firstName.trim().length < 2) {
    errors.firstName = 'Minimum 2 characters';
  }

  // Middle Name (optional but if filled, must be alphabets)
  if (form.middleName && !alphaRegex.test(form.middleName)) {
    errors.middleName = 'Alphabets only';
  }

  // Last Name
  if (!form.lastName.trim()) {
    errors.lastName = 'Last name is required';
  } else if (!alphaRegex.test(form.lastName)) {
    errors.lastName = 'Alphabets only';
  } else if (form.lastName.trim().length < 2) {
    errors.lastName = 'Minimum 2 characters';
  }

  // Date of Birth
  if (!form.day || !form.month || !form.year) {
    errors.dob = 'Complete date of birth is required';
  } else {
    const dob = new Date(parseInt(form.year), MONTHS.indexOf(form.month), parseInt(form.day));
    const cutoff = new Date();
    cutoff.setFullYear(cutoff.getFullYear() - 18);

    if (isNaN(dob.getTime())) {
      errors.dob = 'Invalid date selected';
    } else if (dob > cutoff) {
      errors.dob = 'You must be at least 18 years old to register';
    }
  }

  // Gender
  if (!form.gender) {
    errors.gender = 'Please select a gender';
  }

  // Mobile Number
  if (!form.mobile.trim()) {
    errors.mobile = 'Mobile number is required';
  } else if (!/^\+?[\d]{10,15}$/.test(form.mobile.trim().replace(/\s/g, ''))) {
    errors.mobile = 'Enter a valid 10–15 digit mobile number';
  }

  // Email
  if (!form.email.trim()) {
    errors.email = 'Email address is required';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    errors.email = 'Enter a valid email address';
  }

  // Password
  if (!form.password) {
    errors.password = 'Password is required';
  } else {
    const missing: string[] = [];
    if (form.password.length < 8) missing.push('8+ characters');
    if (!/[A-Z]/.test(form.password)) missing.push('uppercase letter');
    if (!/[a-z]/.test(form.password)) missing.push('lowercase letter');
    if (!/\d/.test(form.password)) missing.push('number');
    if (!/[^A-Za-z0-9]/.test(form.password)) missing.push('special character');
    if (missing.length) {
      errors.password = `Must include: ${missing.join(', ')}`;
    }
  }

  // Confirm Password
  if (!form.confirmPassword) {
    errors.confirmPassword = 'Please confirm your password';
  } else if (form.password !== form.confirmPassword) {
    errors.confirmPassword = 'Passwords do not match';
  }

  // Terms
  if (!form.terms) {
    errors.terms = 'You must accept the Terms & Conditions to continue';
  }

  return errors;
}

// ─── Password Strength Indicator ──────────────────────────────────────────

function PasswordStrengthIndicator({ password }: { password: string }) {
  const { score, label, color } = getPasswordStrength(password);

  if (!password) return null;

  return (
    <Stack spacing={1} sx={{ mt: 1.5 }}>
      <Box sx={{ display: 'flex', gap: 1 }}>
        {([1, 2, 3] as const).map((level) => (
          <Box
            key={level}
            sx={{
              flex: 1,
              height: 6,
              borderRadius: '3px',
              backgroundColor: score >= level ? color : '#E5E7EB',
              transition: 'all 300ms',
            }}
          />
        ))}
      </Box>
      <Typography
        variant="caption"
        sx={{
          fontWeight: 600,
          color: color,
        }}
      >
        {label} password
      </Typography>
    </Stack>
  );
}

// ─── Success State ────────────────────────────────────────────────────────

function SuccessState({ firstName, lastName }: { firstName: string; lastName: string }) {
  const navigate = useNavigate();

  return (
    <Stack spacing={3} alignItems="center" sx={{ py: 4 }}>
      <Box
        sx={{
          width: 64,
          height: 64,
          borderRadius: '50%',
          bgcolor: '#E8F5E9',
          border: '2px solid #C8E6C9',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <CheckCircle sx={{ fontSize: 32, color: '#4CAF50' }} />
      </Box>

      <Typography
        variant="h6"
        sx={{
          fontWeight: 600,
          color: '#1A0800',
          fontFamily: "'Playfair Display', Georgia, serif",
        }}
      >
        Account Created!
      </Typography>

      <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
        Welcome, <strong>{`${firstName} ${lastName}`.trim()}</strong>!
      </Typography>

      <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
        Your account has been successfully created.
        <br />
        You can now sign in with your email or mobile number.
      </Typography>

      <Button
        variant="contained"
        onClick={() => navigate('/login')}
        sx={{
          mt: 2,
          bgcolor: '#1877F2',
          textTransform: 'none',
          fontWeight: 600,
          '&:hover': {
            bgcolor: '#166FE5',
          },
        }}
      >
        Proceed to Sign In →
      </Button>
    </Stack>
  );
}

// ─── Registration Form ──────────────────────────────────────────────────────

function RegistrationForm() {
  const navigate = useNavigate();
  const [formState, setFormState] = useState<RegistrationFormState>({
    firstName: '',
    middleName: '',
    lastName: '',
    day: '',
    month: '',
    year: '',
    gender: '',
    mobile: '',
    email: '',
    password: '',
    confirmPassword: '',
    terms: false,
  });

  const [errors, setErrors] = useState<RegistrationErrors>({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const setField = <K extends keyof RegistrationFormState>(key: K, value: RegistrationFormState[K]) => {
    setFormState((prev) => ({ ...prev, [key]: value }));
    setErrors((prev) => {
      const newErrors = { ...prev };
      delete newErrors[key];
      if (key === 'day' || key === 'month' || key === 'year') {
        delete newErrors.dob;
      }
      return newErrors;
    });
  };

  if (success) {
    return <SuccessState firstName={formState.firstName} lastName={formState.lastName} />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const formErrors = validateRegistrationForm(formState);

    if (Object.keys(formErrors).length > 0) {
      setErrors(formErrors);
      return;
    }

    setErrors({});
    setSubmitError(null);
    setLoading(true);

    try {
      const dateOfBirth = `${formState.year}-${String(MONTHS.indexOf(formState.month) + 1).padStart(2, '0')}-${formState.day}`;
      await registrationApi.register({
        firstName: formState.firstName.trim(),
        middleName: formState.middleName.trim() || undefined,
        lastName: formState.lastName.trim(),
        dateOfBirth,
        gender: formState.gender as Gender,
        mobileNumber: formState.mobile.trim(),
        emailAddress: formState.email.trim(),
        password: formState.password,
      });
      setSuccess(true);
    } catch (error) {
      const message =
        axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
          ? error.response.data.message
          : 'Registration failed. Please try again.';
      setSubmitError(message);
    } finally {
      setLoading(false);
    }
  };

  const passwordsMatch =
    formState.confirmPassword.length > 0 && formState.password === formState.confirmPassword;

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      noValidate
      sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}
    >
      {/* Section: Full Name */}
      <Box>
        <Typography
          variant="caption"
          sx={{
            fontWeight: 600,
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
            color: 'text.secondary',
            display: 'block',
            mb: 2,
          }}
        >
          Full Name
        </Typography>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField
              fullWidth
              label="First Name"
              placeholder="Enter First Name"
              value={formState.firstName}
              onChange={(e) => setField('firstName', e.target.value)}
              error={!!errors.firstName}
              helperText={errors.firstName}
              autoComplete="given-name"
              size="medium"
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: 50,
                  backgroundColor: '#FFFFFF',
                },
              }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField
              fullWidth
              label="Middle Name"
              placeholder="Enter Middle Name"
              value={formState.middleName}
              onChange={(e) => setField('middleName', e.target.value)}
              error={!!errors.middleName}
              helperText={errors.middleName}
              autoComplete="additional-name"
              size="medium"
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: 50,
                  backgroundColor: '#FFFFFF',
                },
              }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField
              fullWidth
              label="Last Name"
              placeholder="Enter Last Name"
              value={formState.lastName}
              onChange={(e) => setField('lastName', e.target.value)}
              error={!!errors.lastName}
              helperText={errors.lastName}
              autoComplete="family-name"
              size="medium"
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: 50,
                  backgroundColor: '#FFFFFF',
                },
              }}
            />
          </Grid>
        </Grid>
      </Box>

      {/* Section: Date of Birth */}
      <Box>
        <Typography
          variant="caption"
          sx={{
            fontWeight: 600,
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
            color: 'text.secondary',
            display: 'block',
            mb: 2,
          }}
        >
          Date of Birth
        </Typography>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField
              select
              fullWidth
              label="Day"
              value={formState.day}
              onChange={(e) => setField('day', e.target.value)}
              error={!!errors.dob}
              size="medium"
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: 50,
                  backgroundColor: '#FFFFFF',
                },
              }}
            >
              {DAYS.map((d) => (
                <MenuItem key={d} value={d}>
                  {d}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField
              select
              fullWidth
              label="Month"
              value={formState.month}
              onChange={(e) => setField('month', e.target.value)}
              error={!!errors.dob}
              size="medium"
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: 50,
                  backgroundColor: '#FFFFFF',
                },
              }}
            >
              {MONTHS.map((m) => (
                <MenuItem key={m} value={m}>
                  {m}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField
              select
              fullWidth
              label="Year"
              value={formState.year}
              onChange={(e) => setField('year', e.target.value)}
              error={!!errors.dob}
              size="medium"
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: 50,
                  backgroundColor: '#FFFFFF',
                },
              }}
            >
              {YEARS.map((y) => (
                <MenuItem key={y} value={y}>
                  {y}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
        </Grid>
        {errors.dob && (
          <Alert severity="error" sx={{ mt: 1.5, fontSize: '12px' }}>
            {errors.dob}
          </Alert>
        )}
      </Box>

      {/* Gender */}
      <TextField
        select
        fullWidth
        label="Gender"
        value={formState.gender}
        onChange={(e) => setField('gender', e.target.value as Gender)}
        error={!!errors.gender}
        helperText={errors.gender}
        size="medium"
        sx={{
          '& .MuiOutlinedInput-root': {
            height: 50,
            backgroundColor: '#FFFFFF',
          },
        }}
      >
        {GENDERS.map((g) => (
          <MenuItem key={g.value} value={g.value}>
            {g.label}
          </MenuItem>
        ))}
      </TextField>

      {/* Section: Contact Details */}
      <Box>
        <Typography
          variant="caption"
          sx={{
            fontWeight: 600,
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
            color: 'text.secondary',
            display: 'block',
            mb: 2,
          }}
        >
          Contact Details
        </Typography>
        <Stack spacing={2}>
          <TextField
            fullWidth
            label="Mobile Number"
            placeholder="Enter Mobile Number"
            value={formState.mobile}
            onChange={(e) => setField('mobile', e.target.value.replace(/[^\d+\s]/g, ''))}
            error={!!errors.mobile}
            helperText={errors.mobile}
            inputMode="numeric"
            autoComplete="tel-national"
            size="medium"
            InputProps={{
              startAdornment: <InputAdornment position="start">+91</InputAdornment>,
            }}
            sx={{
              '& .MuiOutlinedInput-root': {
                height: 50,
                backgroundColor: '#FFFFFF',
              },
            }}
          />

          <TextField
            fullWidth
            type="email"
            label="Email Address"
            placeholder="Enter Email Address"
            value={formState.email}
            onChange={(e) => setField('email', e.target.value)}
            error={!!errors.email}
            helperText={errors.email}
            autoComplete="email"
            size="medium"
            sx={{
              '& .MuiOutlinedInput-root': {
                height: 50,
                backgroundColor: '#FFFFFF',
              },
            }}
          />
        </Stack>
      </Box>

      {/* Section: Set Password */}
      <Box>
        <Typography
          variant="caption"
          sx={{
            fontWeight: 600,
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
            color: 'text.secondary',
            display: 'block',
            mb: 2,
          }}
        >
          Set Password
        </Typography>
        <Stack spacing={2}>
          <Box>
            <TextField
              fullWidth
              type="password"
              label="Password"
              placeholder="Create Password"
              value={formState.password}
              onChange={(e) => setField('password', e.target.value)}
              error={!!errors.password}
              helperText={errors.password}
              inputProps={{ maxLength: 50 }}
              autoComplete="new-password"
              size="medium"
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: 50,
                  backgroundColor: '#FFFFFF',
                },
              }}
            />
            <PasswordStrengthIndicator password={formState.password} />
          </Box>

          <Box>
            <TextField
              fullWidth
              type="password"
              label="Confirm Password"
              placeholder="Confirm Password"
              value={formState.confirmPassword}
              onChange={(e) => setField('confirmPassword', e.target.value)}
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword}
              inputProps={{ maxLength: 50 }}
              autoComplete="new-password"
              size="medium"
              InputProps={{
                endAdornment: passwordsMatch ? (
                  <InputAdornment position="end">
                    <CheckCircle sx={{ fontSize: 20, color: '#42B72A' }} />
                  </InputAdornment>
                ) : undefined,
              }}
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: 50,
                  backgroundColor: '#FFFFFF',
                },
              }}
            />
          </Box>
        </Stack>
      </Box>

      {/* Divider */}
      <Divider sx={{ my: 1 }} />

      {/* Terms & Conditions */}
      <FormControlLabel
        control={
          <Checkbox
            checked={formState.terms}
            onChange={(e) => setField('terms', e.target.checked)}
            sx={{
              '&.Mui-checked': {
                color: 'primary.main',
              },
            }}
          />
        }
        label={
          <Typography variant="body2">
            I agree to the{' '}
            <Link
              component="button"
              type="button"
              onClick={(e) => {
                e.preventDefault();
              }}
              sx={{
                textDecoration: 'none',
                fontWeight: 500,
                '&:hover': {
                  textDecoration: 'underline',
                },
              }}
            >
              Terms & Conditions
            </Link>
            {' '}and{' '}
            <Link
              component="button"
              type="button"
              onClick={(e) => {
                e.preventDefault();
              }}
              sx={{
                textDecoration: 'none',
                fontWeight: 500,
                '&:hover': {
                  textDecoration: 'underline',
                },
              }}
            >
              Privacy Policy
            </Link>
            .
          </Typography>
        }
        sx={{
          minHeight: 44,
          ml: -1.25,
          alignItems: 'flex-start',
        }}
      />

      {errors.terms && (
        <Alert severity="error" sx={{ fontSize: '12px' }}>
          {errors.terms}
        </Alert>
      )}

      {submitError && <Alert severity="error">{submitError}</Alert>}

      {/* Submit Button */}
      <Button
        type="submit"
        fullWidth
        variant="contained"
        disabled={loading}
        sx={{
          height: 50,
          bgcolor: '#42B72A',
          color: 'white',
          fontWeight: 600,
          fontSize: '15px',
          textTransform: 'none',
          borderRadius: '8px',
          '&:hover': {
            bgcolor: '#36A420',
          },
          '&:active': {
            bgcolor: '#2E8F1B',
          },
          '&:disabled': {
            opacity: 0.6,
            cursor: 'not-allowed',
          },
          transition: 'all 200ms',
        }}
      >
        {loading ? (
          <Stack direction="row" alignItems="center" spacing={1}>
            <CircularProgress size={16} sx={{ color: 'inherit' }} />
            <span>Creating Account…</span>
          </Stack>
        ) : (
          'Create Account'
        )}
      </Button>

      {/* Sign In Link */}
      <Typography variant="body2" sx={{ textAlign: 'center', color: '#65676B' }}>
        Already have an account?{' '}
        <Link
          component="button"
          type="button"
          onClick={(e) => {
            e.preventDefault();
            navigate('/login');
          }}
          sx={{
            fontWeight: 600,
            textDecoration: 'none',
            '&:hover': {
              textDecoration: 'underline',
            },
          }}
        >
          Sign In
        </Link>
      </Typography>
    </Box>
  );
}

// ─── Registration Card ───────────────────────────────────────────────────

function RegistrationCard() {
  return (
    <Paper
      elevation={0}
      sx={{
        width: '100%',
        maxWidth: '600px',
        padding: { xs: 3, sm: 4 },
        borderRadius: '12px',
        boxShadow: '0px 4px 20px rgba(0, 0, 0, 0.10)',
        backgroundColor: '#FFFFFF',
        transition: 'box-shadow 300ms cubic-bezier(0.4, 0, 0.2, 1)',
        my: { xs: 3, sm: 0 },
        '&:hover': {
          boxShadow: '0px 8px 32px rgba(0, 0, 0, 0.14)',
        },
      }}
    >
      {/* Brand Header */}
      <Stack direction="row" alignItems="center" spacing={1.5} sx={{ mb: 3, justifyContent: 'center' }}>
        <Box
          sx={{
            width: 32,
            height: 32,
            borderRadius: '50%',
            backgroundColor: '#7C1D1D',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <Typography
            sx={{
              color: 'white',
              fontWeight: 700,
              fontSize: '14px',
              fontFamily: "'Playfair Display', Georgia, serif",
            }}
          >
            A
          </Typography>
        </Box>
        <Typography
          sx={{
            fontSize: '14px',
            fontWeight: 600,
            color: '#65676B',
            letterSpacing: '0.5px',
          }}
        >
          AgrawalPulse
        </Typography>
      </Stack>

      {/* Heading */}
      <Typography
        variant="h4"
        sx={{
          textAlign: 'center',
          fontWeight: 700,
          color: '#1A0800',
          mb: 0.5,
          fontFamily: "'Playfair Display', Georgia, serif",
          fontSize: '30px',
          lineHeight: 1.4,
        }}
      >
        Create New Account
      </Typography>

      {/* Subtitle */}
      <Typography
        variant="body2"
        sx={{
          textAlign: 'center',
          color: '#65676B',
          mb: 3,
          fontSize: '14px',
        }}
      >
        Please fill in the information below to create your account
      </Typography>

      {/* Form */}
      <RegistrationForm />
    </Paper>
  );
}

// ─── Registration Page ────────────────────────────────────────────────────

export default function RegistrationPage() {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        width: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: { xs: 2, sm: 3 },
        py: { xs: 2, sm: 4 },
        backgroundColor: '#F0F2F5',
      }}
    >
      <main style={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
        <RegistrationCard />
      </main>
    </Box>
  );
}

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '@/auth/useAuth';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import Typography from '@mui/material/Typography';
import Link from '@mui/material/Link';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Paper from '@mui/material/Paper';
import CircularProgress from '@mui/material/CircularProgress';

// ─── Validation Functions ───────────────────────────────────────────────────

function validateIdentifier(value: string): string | undefined {
  if (!value.trim()) return 'Email or mobile number is required';
  const isEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
  const isMobile = /^\+?[\d]{10,15}$/.test(value.trim().replace(/\s/g, ''));
  if (!isEmail && !isMobile) return 'Enter a valid email address or 10–15 digit mobile number';
  return undefined;
}

function validatePassword(value: string): string | undefined {
  if (!value) return 'Password is required';
  if (value.length < 8) return 'Password must be at least 8 characters';
  if (value.length > 50) return 'Password must be under 50 characters';
  return undefined;
}

// ─── Login Form Component ──────────────────────────────────────────────────

function LoginForm() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [errors, setErrors] = useState<{ identifier?: string; password?: string }>({});
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const idErr = validateIdentifier(identifier);
    const pwErr = validatePassword(password);

    if (idErr || pwErr) {
      setErrors({ identifier: idErr, password: pwErr });
      return;
    }

    setErrors({});
    setLoading(true);

    try {
      // Stores the token and sets auth state. The backend detects whether `identifier` is an
      // email or a mobile number (DbLocalCredentialAuthenticator) - chapter and role are resolved
      // from the account server-side, so there is nothing else to send.
      await login(identifier.trim(), password);

      // Straight to the dashboard - no interstitial. A "Login Successful" screen the user can't
      // act on only delays them, and the dashboard appearing is itself the confirmation.
      navigate('/dashboard', { replace: true });
    } catch (error) {
      setLoading(false);

      let detail = error instanceof Error ? error.message : String(error);
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 401 && typeof error.response.data?.message === 'string') {
          // DbLocalCredentialAuthenticator's three rejection reasons ("User account does not
          // exist...", "Your account is inactive...", "Invalid username or password.") are
          // already written to be shown to the user as-is - no "HTTP 401:" prefix needed.
          detail = error.response.data.message;
        } else if (error.response) {
          // Any other status - show status + whatever body it sent back.
          const body =
            typeof error.response.data === 'string'
              ? error.response.data
              : JSON.stringify(error.response.data);
          detail = `HTTP ${error.response.status}: ${body}`;
        } else if (error.request) {
          // Request was sent but no response came back at all (backend not running / MSW not
          // intercepting / wrong port).
          detail = `No response from ${error.config?.baseURL ?? ''}${error.config?.url ?? ''} - is the backend running, or is mock mode enabled (npm run dev:mock)?`;
        }
      }

      console.error('Login error - full detail:', detail, error);

      setErrors({
        identifier: detail,
      });
    }
  };

  const handleTogglePassword = () => {
    setShowPassword(!showPassword);
  };

  const handleIdentifierChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setIdentifier(e.target.value);
    setErrors((p) => ({ ...p, identifier: undefined }));
  };

  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPassword(e.target.value);
    setErrors((p) => ({ ...p, password: undefined }));
  };

  const looksNumeric = /^[+\d]/.test(identifier);

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      noValidate
      sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}
    >
      {/* Email or Mobile Number */}
      <TextField
        fullWidth
        label="Email or Mobile Number"
        placeholder="Enter email address or mobile number"
        type="text"
        value={identifier}
        onChange={handleIdentifierChange}
        error={!!errors.identifier}
        helperText={errors.identifier}
        inputMode={looksNumeric ? 'numeric' : 'email'}
        autoComplete="username"
        size="medium"
        sx={{
          '& .MuiOutlinedInput-root': {
            height: 50,
            backgroundColor: '#FFFFFF',
            borderRadius: '8px',
          },
          '& .MuiOutlinedInput-input': {
            fontSize: '14px',
          },
        }}
      />

      {/* Password */}
      <TextField
        fullWidth
        label="Password"
        placeholder="Enter password"
        type={showPassword ? 'text' : 'password'}
        value={password}
        onChange={handlePasswordChange}
        error={!!errors.password}
        helperText={errors.password}
        autoComplete="current-password"
        inputProps={{ maxLength: 50 }}
        size="medium"
        sx={{
          '& .MuiOutlinedInput-root': {
            height: 50,
            backgroundColor: '#FFFFFF',
            borderRadius: '8px',
          },
          '& .MuiOutlinedInput-input': {
            fontSize: '14px',
          },
        }}
        InputProps={{
          endAdornment: (
            <InputAdornment position="end">
              <IconButton
                onClick={handleTogglePassword}
                edge="end"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                size="small"
                sx={{
                  color: 'text.secondary',
                  '&:hover': { color: 'text.primary' },
                }}
              >
                {showPassword ? (
                  <VisibilityOff fontSize="small" />
                ) : (
                  <Visibility fontSize="small" />
                )}
              </IconButton>
            </InputAdornment>
          ),
        }}
      />

      {/* Remember Me */}
      <FormControlLabel
        control={
          <Checkbox
            checked={rememberMe}
            onChange={(e) => setRememberMe(e.target.checked)}
            sx={{
              '&.Mui-checked': {
                color: 'primary.main',
              },
            }}
          />
        }
        label={
          <Typography variant="body2" sx={{ color: '#65676B' }}>
            Remember Me
          </Typography>
        }
        sx={{
          minHeight: 44,
          marginX: -1,
          marginY: -0.5,
        }}
      />

      {/* Login Button */}
      <Button
        type="submit"
        fullWidth
        variant="contained"
        disabled={loading}
        sx={{
          height: 50,
          bgcolor: '#1877F2',
          color: 'white',
          fontWeight: 600,
          fontSize: '15px',
          textTransform: 'none',
          borderRadius: '8px',
          '&:hover': {
            bgcolor: '#166FE5',
          },
          '&:active': {
            bgcolor: '#1469D5',
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
            <span>Signing In…</span>
          </Stack>
        ) : (
          'Login'
        )}
      </Button>

      {/* Forgot Password Link */}
      <Box sx={{ textAlign: 'center' }}>
        <Link
          component="button"
          type="button"
          variant="body2"
          onClick={(e) => {
            e.preventDefault();
            navigate('/forgot-password');
          }}
          sx={{
            color: '#1877F2',
            textDecoration: 'none',
            fontWeight: 500,
            fontSize: '14px',
            minHeight: 44,
            minWidth: 44,
            display: 'inline-flex',
            alignItems: 'center',
            padding: '8px',
            '&:hover': {
              textDecoration: 'underline',
            },
          }}
        >
          Forgot Password?
        </Link>
      </Box>

      {/* OR Divider */}
      <Divider
        sx={{
          my: 1,
          '& .MuiDivider-wrapper': {
            px: 1.5,
            fontSize: '12px',
            fontWeight: 600,
            color: '#BDBDBD',
            letterSpacing: '0.05em',
          },
        }}
      >
        OR
      </Divider>

      {/* Create Account Button */}
      <Button
        type="button"
        fullWidth
        variant="contained"
        onClick={() => navigate('/register')}
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
          transition: 'all 200ms',
        }}
      >
        Create Account
      </Button>
    </Box>
  );
}

// ─── Login Card Component ──────────────────────────────────────────────────

function LoginCard() {
  return (
    <Paper
      elevation={0}
      sx={{
        width: '100%',
        maxWidth: '420px',
        padding: { xs: 3, sm: 4 },
        borderRadius: '12px',
        boxShadow: '0px 4px 20px rgba(0, 0, 0, 0.10)',
        backgroundColor: '#FFFFFF',
        transition: 'box-shadow 300ms cubic-bezier(0.4, 0, 0.2, 1)',
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
        Sign In
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
        Login to your account
      </Typography>

      {/* Form */}
      <LoginForm />
    </Paper>
  );
}

// ─── Login Page Component ──────────────────────────────────────────────────

export default function LoginPage() {
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
        <LoginCard />
      </main>
    </Box>
  );
}

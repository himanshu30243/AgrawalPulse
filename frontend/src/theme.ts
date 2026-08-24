import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#7C1D1D',
      contrastText: '#FFFFFF',
      light: '#A84444',
      dark: '#5A1515',
    },
    secondary: {
      main: '#C05B0B',
      contrastText: '#FFFFFF',
      light: '#D97D2D',
      dark: '#903D08',
    },
    success: { main: '#059669' },
    warning: { main: '#D97706' },
    error: {
      main: '#C0392B',
      contrastText: '#FFFFFF',
    },
    background: {
      default: '#FDF8F0',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#1A0800',
      secondary: '#7A5A44',
    },
    divider: 'rgba(124, 29, 29, 0.15)',
    action: {
      disabled: '#F0E8D8',
      disabledBackground: '#F0E8D8',
    },
  },
  typography: {
    fontFamily: '"DM Sans", system-ui, -apple-system, "Segoe UI", sans-serif',
    h1: {
      fontFamily: '"Playfair Display", Georgia, serif',
      fontWeight: 600,
    },
    h2: {
      fontFamily: '"Playfair Display", Georgia, serif',
      fontWeight: 600,
    },
    h3: {
      fontFamily: '"Playfair Display", Georgia, serif',
      fontWeight: 600,
    },
    h4: {
      fontFamily: '"Playfair Display", Georgia, serif',
      fontWeight: 500,
    },
    h5: {
      fontFamily: '"Playfair Display", Georgia, serif',
      fontWeight: 500,
    },
    h6: {
      fontFamily: '"Playfair Display", Georgia, serif',
      fontWeight: 500,
    },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          boxShadow: 'none',
          '&:hover': {
            boxShadow: '0 2px 8px rgba(124, 29, 29, 0.12)',
          },
        },
        containedPrimary: {
          background: 'linear-gradient(135deg, #7C1D1D 0%, #A84444 100%)',
        },
        containedSecondary: {
          background: 'linear-gradient(135deg, #C05B0B 0%, #D97D2D 100%)',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 1px 3px rgba(26, 8, 0, 0.1)',
          border: '1px solid rgba(124, 29, 29, 0.15)',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            backgroundColor: '#FFFFFF',
            '&:hover fieldset': {
              borderColor: '#7C1D1D',
            },
            '&.Mui-focused fieldset': {
              borderColor: '#7C1D1D',
              borderWidth: 2,
            },
          },
        },
      },
    },
    MuiStepIcon: {
      styleOverrides: {
        root: {
          color: '#FFFFFF',
          border: '2px solid rgba(124, 29, 29, 0.15)',
          '&.Mui-active': {
            color: '#7C1D1D',
            boxShadow: '0 0 0 4px rgba(124, 29, 29, 0.1)',
          },
          '&.Mui-completed': {
            color: '#7C1D1D',
          },
        },
      },
    },
    MuiStepper: {
      styleOverrides: {
        root: {
          backgroundColor: 'transparent',
          padding: 0,
        },
      },
    },
  },
});

/**
 * Fixed-order categorical hues for chart series (dataviz palette, light surface).
 * Order matters for colorblind-safety; never cycle or reassign per-filter.
 */
export const chartColors = [
  '#2a78d6', // blue
  '#eb6834', // orange
  '#1baf7a', // aqua
  '#eda100', // yellow
  '#e87ba4', // magenta
  '#4a3aa7', // violet
] as const;

export const statusColors = {
  good: '#0ca30c',
  warning: '#fab219',
  serious: '#ec835a',
  critical: '#d03b3b',
} as const;

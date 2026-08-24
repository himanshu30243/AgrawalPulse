import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';

interface LoadingErrorStateProps {
  isLoading: boolean;
  error: unknown;
  onRetry: () => void;
}

export function LoadingErrorState({ isLoading, error, onRetry }: LoadingErrorStateProps) {
  const { t } = useTranslation();

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert
        severity="error"
        action={
          <Button color="inherit" size="small" onClick={onRetry}>
            {t('common.retry')}
          </Button>
        }
        sx={{ my: 2 }}
      >
        {t('common.errorGeneric')}
      </Alert>
    );
  }

  return null;
}

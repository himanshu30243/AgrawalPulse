import { useTranslation } from 'react-i18next';
import { Link as RouterLink } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';

export default function NotFoundPage() {
  const { t } = useTranslation();

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
      <Stack spacing={2} alignItems="center">
        <Typography variant="h3" fontWeight={700}>
          404
        </Typography>
        <Typography color="text.secondary">Page not found</Typography>
        <Button component={RouterLink} to="/dashboard" variant="contained">
          {t('nav.dashboard')}
        </Button>
      </Stack>
    </Box>
  );
}

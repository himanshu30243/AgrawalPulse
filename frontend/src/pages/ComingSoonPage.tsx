import { useTranslation } from 'react-i18next';
import { Link as RouterLink } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import ConstructionIcon from '@mui/icons-material/Construction';

interface ComingSoonPageProps {
  /** The nav label for the page that isn't built yet, e.g. "Reports". */
  title: string;
}

// Placeholder for a menu entry that's seeded/linked but has no page behind it yet (see
// AppRoutes.tsx). Distinct from NotFoundPage: this is a known, intentional gap, not a bad URL.
export default function ComingSoonPage({ title }: ComingSoonPageProps) {
  const { t } = useTranslation();

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
      <Stack spacing={2} alignItems="center" sx={{ textAlign: 'center' }}>
        <ConstructionIcon sx={{ fontSize: 48 }} color="disabled" />
        <Typography variant="h5" fontWeight={700}>
          {title}
        </Typography>
        <Typography color="text.secondary">This page is under construction.</Typography>
        <Button component={RouterLink} to="/dashboard" variant="contained">
          {t('nav.dashboard')}
        </Button>
      </Stack>
    </Box>
  );
}

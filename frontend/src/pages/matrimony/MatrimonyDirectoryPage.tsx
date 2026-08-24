import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Paper from '@mui/material/Paper';
import Alert from '@mui/material/Alert';
import Chip from '@mui/material/Chip';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableBody from '@mui/material/TableBody';
import TableContainer from '@mui/material/TableContainer';
import InfoIcon from '@mui/icons-material/Info';
import { matrimonyApi } from '@/api/matrimonyApi';
import { useAsync } from '@/hooks/useAsync';
import { LoadingErrorState } from '@/components/LoadingErrorState';

/**
 * Gated behind MATRIMONY_VIEWER (+ NATIONAL_ADMIN) — mirrors
 * GET /matrimony/eligible in docs/api-specifications.md, which is the only
 * matrimony endpoint that actually requires the viewer role. Consent capture
 * lives on the separate, ungated MyConsentPage.
 */
export default function MatrimonyDirectoryPage() {
  const { t } = useTranslation();
  const { data, isLoading, error, reload } = useAsync(() => matrimonyApi.getDirectory(), []);

  return (
    <Stack spacing={3}>
      <Stack spacing={0.5}>
        <Typography variant="h5" fontWeight={600}>
          {t('matrimony.title')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t('matrimony.subtitle')}
        </Typography>
      </Stack>

      <Alert severity="info" icon={<InfoIcon />}>
        {t('matrimony.accessTierNote')}
      </Alert>

      <LoadingErrorState isLoading={isLoading} error={error} onRetry={reload} />

      {data && data.length === 0 && <Typography color="text.secondary">{t('common.noData')}</Typography>}

      {data && data.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('common.name')}</TableCell>
                <TableCell>{t('families.chapter')}</TableCell>
                <TableCell>{t('matrimony.district')}</TableCell>
                <TableCell align="right">{t('matrimony.readinessAge')}</TableCell>
                <TableCell>{t('families.education')}</TableCell>
                <TableCell>{t('families.profession')}</TableCell>
                <TableCell>{t('matrimony.consentScope')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {data.map((profile) => (
                <TableRow key={profile.individualId} hover>
                  <TableCell>{profile.fullName}</TableCell>
                  <TableCell>{profile.chapterName}</TableCell>
                  <TableCell>{profile.district}</TableCell>
                  <TableCell align="right">{profile.age}</TableCell>
                  <TableCell>{profile.education}</TableCell>
                  <TableCell>{profile.profession}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={
                        profile.consentScope === 'NATIONAL'
                          ? t('matrimony.consentScopeNational')
                          : t('matrimony.consentScopeChapter')
                      }
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Stack>
  );
}

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Paper from '@mui/material/Paper';
import Chip from '@mui/material/Chip';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid2';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableBody from '@mui/material/TableBody';
import TableContainer from '@mui/material/TableContainer';
import Snackbar from '@mui/material/Snackbar';
import Alert from '@mui/material/Alert';
import type { AlertColor } from '@mui/material/Alert';
import { membershipApi } from '@/api/membershipApi';
import { useAsync } from '@/hooks/useAsync';
import { useAuth } from '@/auth/useAuth';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import type { MembershipStatus } from '@/types/domain';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

const STATUS_COLOR: Record<MembershipStatus, 'success' | 'warning' | 'error'> = {
  ACTIVE: 'success',
  PENDING: 'warning',
  INACTIVE: 'error',
};

function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleDateString('en-IN') : '—';
}

function StatusTab() {
  const { t } = useTranslation();
  // No Family<->User linkage exists yet in this slice, so the signed-in
  // user's id stands in for "their" family id — the backend Membership
  // module will supply the real family lookup once auth <-> family
  // association is modeled.
  const { user } = useAuth();
  const familyId = user?.id ?? '';

  const summary = useAsync(() => membershipApi.getStatus(familyId), [familyId]);
  const history = useAsync(() => membershipApi.getPaymentHistory(familyId), [familyId]);

  const [isRenewing, setIsRenewing] = useState(false);
  const [toast, setToast] = useState<{ severity: AlertColor; message: string } | null>(null);

  const handleRenew = async () => {
    setIsRenewing(true);
    try {
      await membershipApi.renew(familyId);
      setToast({ severity: 'success', message: t('membership.renewSuccess') });
      summary.reload();
      history.reload();
    } catch {
      setToast({ severity: 'error', message: t('membership.renewError') });
    } finally {
      setIsRenewing(false);
    }
  };

  return (
    <Stack spacing={3}>
      <LoadingErrorState isLoading={summary.isLoading} error={summary.error} onRetry={summary.reload} />

      {summary.data && (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Grid container spacing={2} alignItems="center">
            <Grid size={{ xs: 12, sm: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.currentStatus')}
              </Typography>
              <Chip
                label={t(`membership.status${summary.data.status.charAt(0)}${summary.data.status.slice(1).toLowerCase()}`)}
                color={STATUS_COLOR[summary.data.status]}
                sx={{ mt: 0.5 }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.annualFee')}
              </Typography>
              <Typography variant="body1">{inr.format(summary.data.annualFee)}</Typography>
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.lastPaymentDate')}
              </Typography>
              <Typography variant="body1">{formatDate(summary.data.lastPaymentDate)}</Typography>
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.nextDueDate')}
              </Typography>
              <Typography variant="body1">{formatDate(summary.data.nextDueDate)}</Typography>
            </Grid>
          </Grid>
          <Button
            variant="contained"
            sx={{ mt: 3 }}
            disabled={isRenewing || summary.data.status === 'ACTIVE'}
            onClick={handleRenew}
          >
            {isRenewing ? t('membership.renewing') : t('membership.renew')}
          </Button>
        </Paper>
      )}

      <Stack spacing={1}>
        <Typography variant="subtitle1" fontWeight={600}>
          {t('membership.paymentHistory')}
        </Typography>
        <LoadingErrorState isLoading={history.isLoading} error={history.error} onRetry={history.reload} />
        {history.data && history.data.length === 0 && (
          <Typography color="text.secondary">{t('common.noData')}</Typography>
        )}
        {history.data && history.data.length > 0 && (
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('membership.paymentDate')}</TableCell>
                  <TableCell>{t('membership.receiptNo')}</TableCell>
                  <TableCell align="right">{t('membership.amount')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {history.data.map((payment) => (
                  <TableRow key={payment.id} hover>
                    <TableCell>{formatDate(payment.paymentDate)}</TableCell>
                    <TableCell>{payment.receiptNo}</TableCell>
                    <TableCell align="right">{inr.format(payment.amount)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Stack>

      <Snackbar
        open={Boolean(toast)}
        autoHideDuration={4000}
        onClose={() => setToast(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        {toast ? (
          <Alert severity={toast.severity} onClose={() => setToast(null)}>
            {toast.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </Stack>
  );
}

function CollectionTab() {
  const { t } = useTranslation();
  const { data, isLoading, error, reload } = useAsync(() => membershipApi.getChapterCollection(), []);

  return (
    <Stack spacing={2}>
      <LoadingErrorState isLoading={isLoading} error={error} onRetry={reload} />
      {data && data.length === 0 && <Typography color="text.secondary">{t('common.noData')}</Typography>}
      {data && data.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('families.chapter')}</TableCell>
                <TableCell align="right">{t('membership.totalCollected')}</TableCell>
                <TableCell align="right">{t('membership.familiesPaid')}</TableCell>
                <TableCell align="right">{t('membership.familiesPending')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {data.map((row) => (
                <TableRow key={row.chapterId} hover>
                  <TableCell>{row.chapterName}</TableCell>
                  <TableCell align="right">{inr.format(row.totalCollected)}</TableCell>
                  <TableCell align="right">{row.familiesPaid}</TableCell>
                  <TableCell align="right">{row.familiesPending}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Stack>
  );
}

export default function MembershipPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState(0);

  return (
    <Stack spacing={3}>
      <Stack spacing={0.5}>
        <Typography variant="h5" fontWeight={600}>
          {t('membership.title')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t('membership.subtitle')}
        </Typography>
      </Stack>

      <Tabs value={tab} onChange={(_, value: number) => setTab(value)}>
        <Tab label={t('membership.statusTab')} />
        <Tab label={t('membership.collectionTab')} />
      </Tabs>

      {tab === 0 ? <StatusTab /> : <CollectionTab />}
    </Stack>
  );
}

import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Paper from '@mui/material/Paper';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid2';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableBody from '@mui/material/TableBody';
import TableContainer from '@mui/material/TableContainer';
import Button from '@mui/material/Button';
import { familiesApi } from '@/api/familiesApi';
import { membershipApi } from '@/api/membershipApi';
import { useAsync } from '@/hooks/useAsync';
import { useAuth } from '@/auth/useAuth';
import { isOwnFamily } from '@/auth/permissions';
import { useMembershipPermissions } from '@/auth/useMembershipPermissions';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { financialYearLabel } from './financialYear';
import { RecordTransactionDialog } from './RecordTransactionDialog';
import type { MembershipStatus, MembershipTransaction } from '@/types/domain';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

const STATUS_COLOR: Record<MembershipStatus, 'success' | 'warning' | 'error'> = {
  ACTIVE: 'success',
  PENDING_RENEWAL: 'warning',
  EXPIRED: 'error',
};

const STATUS_LABEL_KEY: Record<MembershipStatus, string> = {
  ACTIVE: 'membership.statusActive',
  PENDING_RENEWAL: 'membership.statusPendingRenewal',
  EXPIRED: 'membership.statusExpired',
};

function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleDateString('en-IN') : '—';
}

// Requirement #1: a member sees only their own family's status and transaction history, never
// another family's. Ownership is resolved client-side from the caller's own already-scoped
// familiesApi.list() result (isOwnFamily) rather than a hand-rolled "user id as family id" hack -
// the real isolation guarantee comes from the backend (family-service's own read scope, and
// membership-service's requireFamily delegating to it), this is just picking "mine" out of what
// the backend already limited to what this caller may see.
export function MembershipStatusTab() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const { canManageMembership } = useMembershipPermissions();
  const families = useAsync(() => familiesApi.list(), []);
  const myFamily = useMemo(
    () => families.data?.find((family) => isOwnFamily(family, user)) ?? null,
    [families.data, user],
  );

  const status = useAsync(
    () => (myFamily ? membershipApi.getStatus(myFamily.id) : Promise.resolve(null)),
    [myFamily?.id],
  );
  const history = useAsync(
    () => (myFamily ? membershipApi.getTransactionHistory(myFamily.id) : Promise.resolve([])),
    [myFamily?.id],
  );

  const [editingTransaction, setEditingTransaction] = useState<MembershipTransaction | null>(null);

  if (families.isLoading || families.error) {
    return <LoadingErrorState isLoading={families.isLoading} error={families.error} onRetry={families.reload} />;
  }

  if (!myFamily) {
    return (
      <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">{t('membership.noOwnFamily')}</Typography>
      </Paper>
    );
  }

  return (
    <Stack spacing={3}>
      <LoadingErrorState isLoading={status.isLoading} error={status.error} onRetry={status.reload} />

      {status.data && (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Grid container spacing={2} alignItems="center">
            <Grid size={{ xs: 12, sm: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.currentStatus')}
              </Typography>
              <Chip
                label={t(STATUS_LABEL_KEY[status.data.status])}
                color={STATUS_COLOR[status.data.status]}
                sx={{ mt: 0.5 }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.financialYear')}
              </Typography>
              <Typography variant="body1">{financialYearLabel(status.data.currentFinancialYear)}</Typography>
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.lastPaymentDate')}
              </Typography>
              <Typography variant="body1">{formatDate(status.data.lastPaymentDate)}</Typography>
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.lastPaidFinancialYear')}
              </Typography>
              <Typography variant="body1">
                {status.data.lastPaidFinancialYear ? financialYearLabel(status.data.lastPaidFinancialYear) : '—'}
              </Typography>
            </Grid>
          </Grid>
        </Paper>
      )}

      <Stack spacing={1}>
        <Typography variant="subtitle1" fontWeight={600}>
          {t('membership.transactionHistory')}
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
                  <TableCell>{t('membership.financialYear')}</TableCell>
                  <TableCell>{t('membership.paymentDate')}</TableCell>
                  <TableCell align="right">{t('membership.amount')}</TableCell>
                  <TableCell>{t('membership.paymentMode')}</TableCell>
                  <TableCell>{t('membership.transactionRef')}</TableCell>
                  <TableCell>{t('membership.remarks')}</TableCell>
                  {canManageMembership && <TableCell align="right">{t('common.actions')}</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {history.data.map((transaction) => (
                  <TableRow key={transaction.id} hover>
                    <TableCell>{financialYearLabel(transaction.financialYear)}</TableCell>
                    <TableCell>{formatDate(transaction.paymentDate)}</TableCell>
                    <TableCell align="right">{inr.format(transaction.amount)}</TableCell>
                    <TableCell>{t(`membership.paymentMode${transaction.paymentMode}`)}</TableCell>
                    <TableCell>{transaction.transactionRef || '—'}</TableCell>
                    <TableCell>{transaction.remarks || '—'}</TableCell>
                    {canManageMembership && (
                      <TableCell align="right">
                        <Button size="small" onClick={() => setEditingTransaction(transaction)}>
                          {t('common.edit')}
                        </Button>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Stack>

      {editingTransaction && (
        <RecordTransactionDialog
          initialFamily={myFamily}
          transaction={editingTransaction}
          onClose={() => setEditingTransaction(null)}
          onSaved={() => {
            setEditingTransaction(null);
            status.reload();
            history.reload();
          }}
        />
      )}
    </Stack>
  );
}

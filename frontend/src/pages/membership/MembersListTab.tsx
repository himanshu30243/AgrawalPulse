import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Paper from '@mui/material/Paper';
import Chip from '@mui/material/Chip';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
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
import { useMembershipPermissions } from '@/auth/useMembershipPermissions';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { currentFinancialYear, financialYearLabel, financialYearOptions } from './financialYear';
import { RecordTransactionDialog } from './RecordTransactionDialog';
import type { Family, MembershipStatus } from '@/types/domain';

const ALL = 'ALL';

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

// Admin "Active/Pending/Expired members" listing (requirement #2). The backend only returns
// familyId + computed status (MembershipStatusDto has no display fields) - families are fetched
// separately and joined here client-side, the same "fetch once, join in memory" shape
// FamiliesListPage already uses for branch info, rather than adding a second backend join.
export function MembersListTab() {
  const { t } = useTranslation();
  const { canManageMembership } = useMembershipPermissions();
  const [financialYear, setFinancialYear] = useState(currentFinancialYear());
  const [statusFilter, setStatusFilter] = useState<MembershipStatus | typeof ALL>(ALL);
  const [recordingFor, setRecordingFor] = useState<Family | null>(null);

  const members = useAsync(
    () => membershipApi.listMembers(financialYear, statusFilter === ALL ? undefined : statusFilter),
    [financialYear, statusFilter],
  );
  const families = useAsync(() => familiesApi.list(), []);

  const familiesById = useMemo(() => {
    const map = new Map<string, Family>();
    for (const family of families.data ?? []) map.set(family.id, family);
    return map;
  }, [families.data]);

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
        <TextField
          select
          label={t('membership.financialYear')}
          value={financialYear}
          onChange={(e) => setFinancialYear(Number(e.target.value))}
          size="small"
          sx={{ maxWidth: { sm: 200 } }}
        >
          {financialYearOptions().map((year) => (
            <MenuItem key={year} value={year}>
              {financialYearLabel(year)}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          select
          label={t('common.status')}
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as MembershipStatus | typeof ALL)}
          size="small"
          sx={{ maxWidth: { sm: 200 } }}
        >
          <MenuItem value={ALL}>{t('membership.allStatuses')}</MenuItem>
          <MenuItem value="ACTIVE">{t('membership.statusActive')}</MenuItem>
          <MenuItem value="PENDING_RENEWAL">{t('membership.statusPendingRenewal')}</MenuItem>
          <MenuItem value="EXPIRED">{t('membership.statusExpired')}</MenuItem>
        </TextField>
      </Stack>

      <LoadingErrorState isLoading={members.isLoading} error={members.error} onRetry={members.reload} />

      {members.data && members.data.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">{t('common.noData')}</Typography>
        </Paper>
      )}

      {members.data && members.data.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('families.headOfFamily')}</TableCell>
                <TableCell>{t('families.familyCode')}</TableCell>
                <TableCell>{t('common.status')}</TableCell>
                <TableCell>{t('membership.lastPaidFinancialYear')}</TableCell>
                {canManageMembership && <TableCell align="right">{t('common.actions')}</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {members.data.map((member) => {
                const family = familiesById.get(member.familyId);
                return (
                  <TableRow key={member.familyId} hover>
                    <TableCell>{family?.headOfFamilyName || member.familyId}</TableCell>
                    <TableCell>{family?.familyCode || '—'}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={t(STATUS_LABEL_KEY[member.status])}
                        color={STATUS_COLOR[member.status]}
                      />
                    </TableCell>
                    <TableCell>
                      {member.lastPaidFinancialYear ? financialYearLabel(member.lastPaidFinancialYear) : '—'}
                    </TableCell>
                    {canManageMembership && (
                      <TableCell align="right">
                        <Button size="small" disabled={!family} onClick={() => family && setRecordingFor(family)}>
                          {t('membership.recordTransaction')}
                        </Button>
                      </TableCell>
                    )}
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {recordingFor && (
        <RecordTransactionDialog
          initialFamily={recordingFor}
          onClose={() => setRecordingFor(null)}
          onSaved={() => {
            setRecordingFor(null);
            members.reload();
          }}
        />
      )}
    </Stack>
  );
}

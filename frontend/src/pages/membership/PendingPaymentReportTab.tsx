import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Paper from '@mui/material/Paper';
import Chip from '@mui/material/Chip';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableBody from '@mui/material/TableBody';
import TableContainer from '@mui/material/TableContainer';
import { membershipApi } from '@/api/membershipApi';
import { useAsync } from '@/hooks/useAsync';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { currentFinancialYear, financialYearLabel, financialYearOptions } from './financialYear';
import type { MembershipStatus } from '@/types/domain';

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

// Admin Pending Payment Report (requirement #2), search/filter by Family ID, Family Head Name,
// Mobile Number, Area/Location, Financial Year, Membership Status per the requirement. The first
// five are backend-composed (membershipApi.pendingPaymentReport forwards them to family-service's
// search - see MembershipServiceImpl.pendingPaymentReport); Membership Status is applied
// client-side against the already-fetched rows, since the report endpoint itself already excludes
// ACTIVE families and has no separate status query param to narrow PENDING_RENEWAL vs EXPIRED.
export function PendingPaymentReportTab() {
  const { t } = useTranslation();
  const [financialYear, setFinancialYear] = useState(currentFinancialYear());
  const [familyId, setFamilyId] = useState('');
  const [headOfFamilyName, setHeadOfFamilyName] = useState('');
  const [mobileNumber, setMobileNumber] = useState('');
  const [areaLocality, setAreaLocality] = useState('');
  const [statusFilter, setStatusFilter] = useState<MembershipStatus | typeof ALL>(ALL);

  const report = useAsync(
    () =>
      membershipApi.pendingPaymentReport({
        financialYear,
        familyId: familyId.trim() || undefined,
        headOfFamilyName: headOfFamilyName.trim() || undefined,
        mobileNumber: mobileNumber.trim() || undefined,
        areaLocality: areaLocality.trim() || undefined,
      }),
    [financialYear, familyId, headOfFamilyName, mobileNumber, areaLocality],
  );

  const filtered = useMemo(
    () => (report.data ?? []).filter((row) => statusFilter === ALL || row.status === statusFilter),
    [report.data, statusFilter],
  );

  const clearFilters = () => {
    setFamilyId('');
    setHeadOfFamilyName('');
    setMobileNumber('');
    setAreaLocality('');
    setStatusFilter(ALL);
  };
  const filtersActive =
    familyId !== '' || headOfFamilyName !== '' || mobileNumber !== '' || areaLocality !== '' || statusFilter !== ALL;

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} flexWrap="wrap" alignItems={{ md: 'center' }}>
        <TextField
          select
          label={t('membership.financialYear')}
          value={financialYear}
          onChange={(e) => setFinancialYear(Number(e.target.value))}
          size="small"
          sx={{ maxWidth: { md: 160 } }}
        >
          {financialYearOptions().map((year) => (
            <MenuItem key={year} value={year}>
              {financialYearLabel(year)}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          label={t('membership.familyIdFilter')}
          value={familyId}
          onChange={(e) => setFamilyId(e.target.value)}
          size="small"
          sx={{ maxWidth: { md: 180 } }}
        />
        <TextField
          label={t('families.headOfFamily')}
          value={headOfFamilyName}
          onChange={(e) => setHeadOfFamilyName(e.target.value)}
          size="small"
          sx={{ maxWidth: { md: 200 } }}
        />
        <TextField
          label={t('families.mobileNumber')}
          value={mobileNumber}
          onChange={(e) => setMobileNumber(e.target.value)}
          size="small"
          sx={{ maxWidth: { md: 180 } }}
        />
        <TextField
          label={t('families.areaLocality')}
          value={areaLocality}
          onChange={(e) => setAreaLocality(e.target.value)}
          size="small"
          sx={{ maxWidth: { md: 200 } }}
        />
        <TextField
          select
          label={t('common.status')}
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as MembershipStatus | typeof ALL)}
          size="small"
          sx={{ maxWidth: { md: 180 } }}
        >
          <MenuItem value={ALL}>{t('membership.allStatuses')}</MenuItem>
          <MenuItem value="PENDING_RENEWAL">{t('membership.statusPendingRenewal')}</MenuItem>
          <MenuItem value="EXPIRED">{t('membership.statusExpired')}</MenuItem>
        </TextField>
        {filtersActive && (
          <Button size="small" onClick={clearFilters}>
            {t('families.clearFilters')}
          </Button>
        )}
      </Stack>

      <LoadingErrorState isLoading={report.isLoading} error={report.error} onRetry={report.reload} />

      {report.data && filtered.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">
            {filtersActive ? t('families.noMatches') : t('common.noData')}
          </Typography>
        </Paper>
      )}

      {report.data && filtered.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('families.familyCode')}</TableCell>
                <TableCell>{t('families.headOfFamily')}</TableCell>
                <TableCell>{t('families.mobileNumber')}</TableCell>
                <TableCell>{t('families.areaLocality')}</TableCell>
                <TableCell>{t('common.chapter')}</TableCell>
                <TableCell>{t('common.status')}</TableCell>
                <TableCell>{t('membership.lastPaidFinancialYear')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((row) => (
                <TableRow key={row.familyId} hover>
                  <TableCell>{row.familyCode}</TableCell>
                  <TableCell>{row.headOfFamilyName}</TableCell>
                  <TableCell>{row.mobileNumber || '—'}</TableCell>
                  <TableCell>{row.areaLocality || '—'}</TableCell>
                  <TableCell>{row.chapterName || row.chapterId}</TableCell>
                  <TableCell>
                    <Chip size="small" label={t(STATUS_LABEL_KEY[row.status])} color={STATUS_COLOR[row.status]} />
                  </TableCell>
                  <TableCell>
                    {row.lastPaidFinancialYear ? financialYearLabel(row.lastPaidFinancialYear) : '—'}
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

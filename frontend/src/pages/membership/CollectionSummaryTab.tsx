import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Paper from '@mui/material/Paper';
import Grid from '@mui/material/Grid2';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import { membershipApi } from '@/api/membershipApi';
import { useAsync } from '@/hooks/useAsync';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { currentFinancialYear, financialYearLabel, financialYearOptions } from './financialYear';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

// Admin Membership Collection Summary (requirement #2) - always scoped to the caller's own
// chapter (see CollectionSummaryDto/MembershipServiceImpl.collectionSummary: a single-chapter row,
// not a multi-chapter rollup, regardless of how broad the caller's own read tier is).
export function CollectionSummaryTab() {
  const { t } = useTranslation();
  const [financialYear, setFinancialYear] = useState(currentFinancialYear());
  const summary = useAsync(() => membershipApi.getCollectionSummary(financialYear), [financialYear]);

  return (
    <Stack spacing={3}>
      <TextField
        select
        label={t('membership.financialYear')}
        value={financialYear}
        onChange={(e) => setFinancialYear(Number(e.target.value))}
        size="small"
        sx={{ maxWidth: 200 }}
      >
        {financialYearOptions().map((year) => (
          <MenuItem key={year} value={year}>
            {financialYearLabel(year)}
          </MenuItem>
        ))}
      </TextField>

      <LoadingErrorState isLoading={summary.isLoading} error={summary.error} onRetry={summary.reload} />

      {summary.data && (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
            {summary.data.chapterName || summary.data.chapterId}
          </Typography>
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.totalCollected')}
              </Typography>
              <Typography variant="h6">{inr.format(summary.data.totalCollected)}</Typography>
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.statusActive')}
              </Typography>
              <Typography variant="h6">{summary.data.familiesActive}</Typography>
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.statusPendingRenewal')}
              </Typography>
              <Typography variant="h6">{summary.data.familiesPending}</Typography>
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {t('membership.statusExpired')}
              </Typography>
              <Typography variant="h6">{summary.data.familiesExpired}</Typography>
            </Grid>
          </Grid>
        </Paper>
      )}
    </Stack>
  );
}

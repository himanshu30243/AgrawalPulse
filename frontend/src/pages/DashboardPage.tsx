import { useTranslation } from 'react-i18next';
import Grid from '@mui/material/Grid2';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import { analyticsApi } from '@/api/analyticsApi';
import { useAsync } from '@/hooks/useAsync';
import { useAuth } from '@/auth/useAuth';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { StatTile } from '@/components/StatTile';
import { FamiliesByChapterChart } from '@/components/charts/FamiliesByChapterChart';
import { MembershipStatusChart } from '@/components/charts/MembershipStatusChart';
import { AgeEligibilityChart } from '@/components/charts/AgeEligibilityChart';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

export default function DashboardPage() {
  const { t } = useTranslation();
  const { can } = useAuth();
  const hasReportPermission = can('VIEW_REPORTS');
  const { data, isLoading, error, reload } = useAsync(
    () => hasReportPermission ? analyticsApi.getSummary() : Promise.resolve(null),
    [hasReportPermission]
  );

  return (
    <Stack spacing={3}>
      <Stack spacing={0.5}>
        <Typography variant="h5" fontWeight={600}>
          {t('dashboard.title')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t('dashboard.subtitle')}
        </Typography>
      </Stack>

      {!hasReportPermission && (
        <Alert severity="info">
          {t('dashboard.noReportsPermission') || 'You do not have permission to view analytics and reports. Please contact your administrator for access.'}
        </Alert>
      )}

      {hasReportPermission && (
        <>
          <LoadingErrorState isLoading={isLoading} error={error} onRetry={reload} />

          {data && (
            <>
              <Grid container spacing={2}>
            <Grid size={{ xs: 6, sm: 4, md: 2 }}>
              <StatTile label={t('dashboard.totalFamilies')} value={data.totalFamilies.toLocaleString('en-IN')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 4, md: 2 }}>
              <StatTile label={t('dashboard.totalPopulation')} value={data.totalPopulation.toLocaleString('en-IN')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 4, md: 2 }}>
              <StatTile label={t('dashboard.activeMembers')} value={data.activeMembers.toLocaleString('en-IN')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 4, md: 2 }}>
              <StatTile label={t('dashboard.membershipCollection')} value={inr.format(data.membershipCollection)} />
            </Grid>
            <Grid size={{ xs: 6, sm: 4, md: 2 }}>
              <StatTile label={t('dashboard.eligibleBoys')} value={data.eligibleBoys.toLocaleString('en-IN')} />
            </Grid>
            <Grid size={{ xs: 6, sm: 4, md: 2 }}>
              <StatTile label={t('dashboard.eligibleGirls')} value={data.eligibleGirls.toLocaleString('en-IN')} />
            </Grid>
          </Grid>

          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                  {t('dashboard.familiesByChapter')}
                </Typography>
                <FamiliesByChapterChart data={data.familiesByChapter} />
              </Paper>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                  {t('dashboard.membershipStatusSplit')}
                </Typography>
                <MembershipStatusChart data={data.membershipStatusSplit} />
              </Paper>
            </Grid>
            <Grid size={{ xs: 12 }}>
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                  {t('dashboard.ageEligibilityDistribution')}
                </Typography>
                <AgeEligibilityChart data={data.ageEligibilityDistribution} />
              </Paper>
            </Grid>
          </Grid>
            </>
          )}
        </>
      )}
    </Stack>
  );
}

import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid2';
import Card from '@mui/material/Card';
import CardActionArea from '@mui/material/CardActionArea';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Paper from '@mui/material/Paper';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import InputAdornment from '@mui/material/InputAdornment';
import Button from '@mui/material/Button';
import EventIcon from '@mui/icons-material/Event';
import PlaceIcon from '@mui/icons-material/Place';
import SearchIcon from '@mui/icons-material/Search';
import { eventsApi } from '@/api/eventsApi';
import { familiesApi } from '@/api/familiesApi';
import { useAsync } from '@/hooks/useAsync';
import { useAuth } from '@/auth/useAuth';
import { isOwnFamily } from '@/auth/permissions';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { EventDetailDialog } from './EventDetailDialog';
import type { EventItem, EventTimeframe } from '@/types/domain';

const ALL = 'ALL';

// Member-facing browse - published events only, search/category/timeframe filters, matching
// FamiliesListPage's filter-row convention. The caller's own family (for the Register action) is
// resolved the same way MembershipStatusTab does it: familiesApi.list() (already scoped
// server-side) + isOwnFamily, no separate "am I registered" pre-check endpoint.
export function BrowseEventsTab() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [timeframe, setTimeframe] = useState<EventTimeframe | typeof ALL>('UPCOMING');
  const [selectedEvent, setSelectedEvent] = useState<EventItem | null>(null);

  const events = useAsync(
    () =>
      eventsApi.list({
        search: search.trim() || undefined,
        category: category.trim() || undefined,
        timeframe: timeframe === ALL ? undefined : timeframe,
      }),
    [search, category, timeframe],
  );
  const families = useAsync(() => familiesApi.list(), []);
  const myFamily = useMemo(() => families.data?.find((f) => isOwnFamily(f, user)) ?? null, [families.data, user]);

  const filtersActive = search.trim() !== '' || category.trim() !== '' || timeframe !== 'UPCOMING';
  const clearFilters = () => {
    setSearch('');
    setCategory('');
    setTimeframe('UPCOMING');
  };

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
        <TextField
          label={t('events.search')}
          placeholder={t('events.searchPlaceholder')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          size="small"
          fullWidth
          sx={{ maxWidth: { md: 280 } }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" />
              </InputAdornment>
            ),
          }}
        />
        <TextField
          label={t('events.category')}
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          size="small"
          fullWidth
          sx={{ maxWidth: { md: 200 } }}
        />
        <TextField
          select
          label={t('events.timeframe')}
          value={timeframe}
          onChange={(e) => setTimeframe(e.target.value as EventTimeframe | typeof ALL)}
          size="small"
          sx={{ maxWidth: { md: 200 } }}
        >
          <MenuItem value="UPCOMING">{t('events.upcoming')}</MenuItem>
          <MenuItem value="PAST">{t('events.past')}</MenuItem>
          <MenuItem value={ALL}>{t('events.allTime')}</MenuItem>
        </TextField>
        {filtersActive && (
          <Button size="small" onClick={clearFilters}>
            {t('families.clearFilters')}
          </Button>
        )}
      </Stack>

      <LoadingErrorState isLoading={events.isLoading} error={events.error} onRetry={events.reload} />

      {events.data && events.data.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">
            {filtersActive ? t('events.noMatches') : t('common.noData')}
          </Typography>
        </Paper>
      )}

      {events.data && events.data.length > 0 && (
        <Grid container spacing={2}>
          {events.data.map((event) => (
            <Grid size={{ xs: 12, sm: 6, md: 4 }} key={event.id}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardActionArea sx={{ height: '100%' }} onClick={() => setSelectedEvent(event)}>
                  <CardContent>
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                      <Typography variant="subtitle1" fontWeight={600}>
                        {event.title}
                      </Typography>
                      {event.category && <Chip size="small" label={event.category} />}
                    </Stack>
                    <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 1 }}>
                      <EventIcon fontSize="small" color="action" />
                      <Typography variant="body2" color="text.secondary">
                        {new Date(event.eventDate).toLocaleDateString('en-IN')}
                      </Typography>
                    </Stack>
                    {event.location && (
                      <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                        <PlaceIcon fontSize="small" color="action" />
                        <Typography variant="body2" color="text.secondary">
                          {event.location}
                        </Typography>
                      </Stack>
                    )}
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {selectedEvent && (
        <EventDetailDialog event={selectedEvent} myFamily={myFamily} onClose={() => setSelectedEvent(null)} />
      )}
    </Stack>
  );
}

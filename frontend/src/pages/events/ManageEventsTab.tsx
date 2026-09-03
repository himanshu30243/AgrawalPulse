import { useState } from 'react';
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
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import { eventsApi } from '@/api/eventsApi';
import { useAsync } from '@/hooks/useAsync';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { EventFormDialog } from './EventFormDialog';
import type { EventItem, EventStatus, EventTimeframe } from '@/types/domain';

const ALL = 'ALL';

const STATUS_COLOR: Record<EventStatus, 'default' | 'success' | 'error'> = {
  DRAFT: 'default',
  PUBLISHED: 'success',
  CANCELLED: 'error',
};

// Admin events-management surface (requirement: create/edit/delete/publish/unpublish, any
// status). Filters mirror BrowseEventsTab's plus a status filter, matching
// PendingPaymentReportTab's 5-6-filter row convention.
export function ManageEventsTab() {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [timeframe, setTimeframe] = useState<EventTimeframe | typeof ALL>(ALL);
  const [status, setStatus] = useState<EventStatus | typeof ALL>(ALL);
  const [editingEvent, setEditingEvent] = useState<EventItem | null>(null);
  const [deletingEvent, setDeletingEvent] = useState<EventItem | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const events = useAsync(
    () =>
      eventsApi.listAll({
        search: search.trim() || undefined,
        category: category.trim() || undefined,
        timeframe: timeframe === ALL ? undefined : timeframe,
        status: status === ALL ? undefined : status,
      }),
    [search, category, timeframe, status],
  );

  const handlePublish = async (event: EventItem) => {
    await eventsApi.publish(event.id);
    events.reload();
  };
  const handleUnpublish = async (event: EventItem) => {
    await eventsApi.unpublish(event.id);
    events.reload();
  };
  const handleCancel = async (event: EventItem) => {
    await eventsApi.cancel(event.id);
    events.reload();
  };
  const handleDelete = async () => {
    if (!deletingEvent) return;
    setIsDeleting(true);
    try {
      await eventsApi.remove(deletingEvent.id);
      setDeletingEvent(null);
      events.reload();
    } finally {
      setIsDeleting(false);
    }
  };

  const filtersActive = search.trim() !== '' || category.trim() !== '' || timeframe !== ALL || status !== ALL;
  const clearFilters = () => {
    setSearch('');
    setCategory('');
    setTimeframe(ALL);
    setStatus(ALL);
  };

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} flexWrap="wrap" alignItems={{ md: 'center' }}>
        <TextField
          label={t('events.search')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          size="small"
          sx={{ maxWidth: { md: 220 } }}
        />
        <TextField
          label={t('events.category')}
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          size="small"
          sx={{ maxWidth: { md: 180 } }}
        />
        <TextField
          select
          label={t('events.timeframe')}
          value={timeframe}
          onChange={(e) => setTimeframe(e.target.value as EventTimeframe | typeof ALL)}
          size="small"
          sx={{ maxWidth: { md: 180 } }}
        >
          <MenuItem value={ALL}>{t('events.allTime')}</MenuItem>
          <MenuItem value="UPCOMING">{t('events.upcoming')}</MenuItem>
          <MenuItem value="PAST">{t('events.past')}</MenuItem>
        </TextField>
        <TextField
          select
          label={t('common.status')}
          value={status}
          onChange={(e) => setStatus(e.target.value as EventStatus | typeof ALL)}
          size="small"
          sx={{ maxWidth: { md: 180 } }}
        >
          <MenuItem value={ALL}>{t('events.allStatuses')}</MenuItem>
          <MenuItem value="DRAFT">{t('events.statusDraft')}</MenuItem>
          <MenuItem value="PUBLISHED">{t('events.statusPublished')}</MenuItem>
          <MenuItem value="CANCELLED">{t('events.statusCancelled')}</MenuItem>
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
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('events.eventTitle')}</TableCell>
                <TableCell>{t('events.category')}</TableCell>
                <TableCell>{t('events.eventDate')}</TableCell>
                <TableCell>{t('common.status')}</TableCell>
                <TableCell align="right">{t('common.actions')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {events.data.map((event) => (
                <TableRow key={event.id} hover>
                  <TableCell>{event.title}</TableCell>
                  <TableCell>{event.category || '—'}</TableCell>
                  <TableCell>{new Date(event.eventDate).toLocaleDateString('en-IN')}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={t(`events.status${capitalize(event.status)}`)}
                      color={STATUS_COLOR[event.status]}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={0.5} justifyContent="flex-end" flexWrap="wrap">
                      <Button size="small" onClick={() => setEditingEvent(event)}>
                        {t('common.edit')}
                      </Button>
                      {event.status !== 'PUBLISHED' && (
                        <Button size="small" onClick={() => void handlePublish(event)}>
                          {t('events.publish')}
                        </Button>
                      )}
                      {event.status === 'PUBLISHED' && (
                        <Button size="small" onClick={() => void handleUnpublish(event)}>
                          {t('events.unpublish')}
                        </Button>
                      )}
                      {event.status !== 'CANCELLED' && (
                        <Button size="small" color="warning" onClick={() => void handleCancel(event)}>
                          {t('events.cancel')}
                        </Button>
                      )}
                      <Button size="small" color="error" onClick={() => setDeletingEvent(event)}>
                        {t('common.delete')}
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {editingEvent && (
        <EventFormDialog
          event={editingEvent}
          onClose={() => setEditingEvent(null)}
          onSaved={() => {
            setEditingEvent(null);
            events.reload();
          }}
        />
      )}

      <Dialog open={Boolean(deletingEvent)} onClose={() => setDeletingEvent(null)}>
        <DialogTitle>{t('events.deleteConfirmTitle')}</DialogTitle>
        <DialogContent>
          <Typography>{t('events.deleteConfirmMessage', { title: deletingEvent?.title })}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeletingEvent(null)} disabled={isDeleting}>
            {t('common.cancel')}
          </Button>
          <Button color="error" variant="contained" disabled={isDeleting} onClick={() => void handleDelete()}>
            {t('common.delete')}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

function capitalize(value: string): string {
  return value.charAt(0) + value.slice(1).toLowerCase();
}

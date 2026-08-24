import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid2';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardActions from '@mui/material/CardActions';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Paper from '@mui/material/Paper';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import Snackbar from '@mui/material/Snackbar';
import Alert from '@mui/material/Alert';
import EventIcon from '@mui/icons-material/Event';
import PlaceIcon from '@mui/icons-material/Place';
import { eventsApi } from '@/api/eventsApi';
import { useAsync } from '@/hooks/useAsync';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import type { EventItem } from '@/types/domain';

export default function EventsPage() {
  const { t } = useTranslation();
  const { data, isLoading, error, reload } = useAsync(() => eventsApi.list(), []);

  const [selectedEvent, setSelectedEvent] = useState<EventItem | null>(null);
  const [attendeeCount, setAttendeeCount] = useState('1');
  const [isRegistering, setIsRegistering] = useState(false);
  const [toast, setToast] = useState<{ severity: 'success' | 'error'; message: string } | null>(null);

  const openRegister = (event: EventItem) => {
    setSelectedEvent(event);
    setAttendeeCount('1');
  };

  const handleRegister = async () => {
    if (!selectedEvent) return;
    const count = Number(attendeeCount);
    if (!Number.isInteger(count) || count < 1) return;

    setIsRegistering(true);
    try {
      await eventsApi.register({ eventId: selectedEvent.id, attendeeCount: count });
      setToast({ severity: 'success', message: t('events.registerSuccess') });
      setSelectedEvent(null);
      reload();
    } catch {
      setToast({ severity: 'error', message: t('events.registerError') });
    } finally {
      setIsRegistering(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Stack spacing={0.5}>
        <Typography variant="h5" fontWeight={600}>
          {t('events.title')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t('events.subtitle')}
        </Typography>
      </Stack>

      <LoadingErrorState isLoading={isLoading} error={error} onRetry={reload} />

      {data && data.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">{t('common.noData')}</Typography>
        </Paper>
      )}

      {data && data.length > 0 && (
        <Grid container spacing={2}>
          {data.map((event) => {
            const seatsLeft = event.capacity - event.registeredCount;
            const isFull = seatsLeft <= 0;
            return (
              <Grid size={{ xs: 12, sm: 6, md: 4 }} key={event.id}>
                <Card variant="outlined" sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                  <CardContent sx={{ flexGrow: 1 }}>
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                      <Typography variant="subtitle1" fontWeight={600}>
                        {event.name}
                      </Typography>
                      <Chip size="small" label={event.chapterName} />
                    </Stack>
                    <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 1 }}>
                      <EventIcon fontSize="small" color="action" />
                      <Typography variant="body2" color="text.secondary">
                        {new Date(event.eventDate).toLocaleString('en-IN')}
                      </Typography>
                    </Stack>
                    <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                      <PlaceIcon fontSize="small" color="action" />
                      <Typography variant="body2" color="text.secondary">
                        {event.location}
                      </Typography>
                    </Stack>
                    <Typography variant="body2" sx={{ mt: 1.5 }}>
                      {event.description}
                    </Typography>
                    <Chip
                      size="small"
                      sx={{ mt: 1.5 }}
                      color={isFull ? 'default' : 'success'}
                      label={isFull ? t('events.full') : `${seatsLeft} ${t('events.seatsLeft')}`}
                    />
                  </CardContent>
                  <CardActions>
                    <Button fullWidth variant="contained" disabled={isFull} onClick={() => openRegister(event)}>
                      {t('events.register')}
                    </Button>
                  </CardActions>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}

      <Dialog open={Boolean(selectedEvent)} onClose={() => setSelectedEvent(null)} fullWidth maxWidth="xs">
        <DialogTitle>{selectedEvent?.name}</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label={t('events.attendeeCount')}
            type="number"
            fullWidth
            value={attendeeCount}
            onChange={(e) => setAttendeeCount(e.target.value)}
            slotProps={{ htmlInput: { min: 1, max: selectedEvent ? selectedEvent.capacity - selectedEvent.registeredCount : 1 } }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedEvent(null)} disabled={isRegistering}>
            {t('common.cancel')}
          </Button>
          <Button variant="contained" onClick={handleRegister} disabled={isRegistering}>
            {isRegistering ? t('events.registering') : t('events.register')}
          </Button>
        </DialogActions>
      </Dialog>

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

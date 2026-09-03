import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import EventIcon from '@mui/icons-material/Event';
import PlaceIcon from '@mui/icons-material/Place';
import { eventsApi } from '@/api/eventsApi';
import type { EventItem, Family } from '@/types/domain';

interface Props {
  event: EventItem;
  /** The caller's own family, if any (resolved by the parent tab) - drives the Register action. */
  myFamily: Family | null;
  onClose: () => void;
}

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' });
}

function formatTimeRange(startTime: string, endTime: string): string {
  const format = (time: string) => {
    const [hours = 0, minutes = 0] = time.split(':').map(Number);
    const date = new Date();
    date.setHours(hours, minutes);
    return date.toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' });
  };
  return `${format(startTime)} - ${format(endTime)}`;
}

export function EventDetailDialog({ event, myFamily, onClose }: Props) {
  const { t } = useTranslation();
  const [bannerUrl, setBannerUrl] = useState<string | null>(null);
  const [isRegistering, setIsRegistering] = useState(false);
  // Kept locally rather than closing the dialog on success (the toast lives inside it - closing
  // would hide the confirmation the instant it appears) or re-fetching registration status (no
  // such endpoint exists for a plain member - see the class comment on handleRegister's catch).
  const [hasRegistered, setHasRegistered] = useState(false);
  const [toast, setToast] = useState<{ severity: 'success' | 'error'; message: string } | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (event.hasBanner) {
      eventsApi.getBannerUrl(event.id).then((url) => {
        if (!cancelled) setBannerUrl(url);
      });
    }
    return () => {
      cancelled = true;
    };
  }, [event.id, event.hasBanner]);

  const handleRegister = async () => {
    if (!myFamily) return;
    setIsRegistering(true);
    setToast(null);
    try {
      await eventsApi.register(event.id, { familyId: myFamily.id });
      setToast({ severity: 'success', message: t('events.registerSuccess') });
      setHasRegistered(true);
    } catch (err) {
      // The backend's duplicate-registration check (400) is surfaced as a friendly message here
      // rather than pre-checking registration status via a separate call - see
      // EventServiceImpl.registerFamily's existsByEventIdAndFamilyId check.
      const message =
        axiosErrorMessage(err)?.includes('already registered')
          ? t('events.alreadyRegistered')
          : t('events.registerError');
      setToast({ severity: 'error', message });
    } finally {
      setIsRegistering(false);
    }
  };

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{event.title}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2}>
          {bannerUrl && (
            <img
              src={bannerUrl}
              alt={event.title}
              style={{ width: '100%', maxHeight: 240, objectFit: 'cover', borderRadius: 8 }}
            />
          )}
          {toast && <Alert severity={toast.severity}>{toast.message}</Alert>}

          {event.category && <Chip size="small" label={event.category} />}

          <Stack direction="row" spacing={1} alignItems="center">
            <EventIcon fontSize="small" color="action" />
            <Typography variant="body2">
              {formatDate(event.eventDate)} · {formatTimeRange(event.startTime, event.endTime)}
            </Typography>
          </Stack>

          {event.location && (
            <Stack direction="row" spacing={1} alignItems="center">
              <PlaceIcon fontSize="small" color="action" />
              <Typography variant="body2">{event.location}</Typography>
            </Stack>
          )}

          {event.description && <Typography variant="body2">{event.description}</Typography>}

          {(event.organizerName || event.contactDetails) && (
            <Stack spacing={0.5}>
              <Typography variant="subtitle2">{t('events.organizerName')}</Typography>
              {event.organizerName && <Typography variant="body2">{event.organizerName}</Typography>}
              {event.contactDetails && (
                <Typography variant="body2" color="text.secondary">
                  {event.contactDetails}
                </Typography>
              )}
            </Stack>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common.close')}</Button>
        {myFamily && event.status === 'PUBLISHED' && (
          <Button
            variant="contained"
            disabled={isRegistering || hasRegistered}
            onClick={() => void handleRegister()}
          >
            {isRegistering ? t('events.registering') : t('events.register')}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}

function axiosErrorMessage(err: unknown): string | undefined {
  if (err && typeof err === 'object' && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    return response?.data?.message;
  }
  return undefined;
}

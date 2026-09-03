import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import Typography from '@mui/material/Typography';
import { eventsApi } from '@/api/eventsApi';
import type { EventItem } from '@/types/domain';

interface Props {
  /** Set to edit an existing event instead of creating a new one. */
  event?: EventItem | null;
  onClose: () => void;
  onSaved: () => void;
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

// Single create/edit dialog (isEdit = Boolean(event), exact RecordTransactionDialog.tsx shape).
// Every required field is validated explicitly on Save with a field-level error message + red
// outline (TextField's error/helperText props) rather than silently disabling the Save button -
// same fix already applied to RecordTransactionDialog's Amount field this session.
export function EventFormDialog({ event, onClose, onSaved }: Props) {
  const { t } = useTranslation();
  const isEdit = Boolean(event);

  const [title, setTitle] = useState(event?.title ?? '');
  const [description, setDescription] = useState(event?.description ?? '');
  const [category, setCategory] = useState(event?.category ?? '');
  const [eventDate, setEventDate] = useState(event?.eventDate ?? todayIso());
  const [startTime, setStartTime] = useState(event?.startTime.slice(0, 5) ?? '09:00');
  const [endTime, setEndTime] = useState(event?.endTime.slice(0, 5) ?? '17:00');
  const [location, setLocation] = useState(event?.location ?? '');
  const [organizerName, setOrganizerName] = useState(event?.organizerName ?? '');
  const [contactDetails, setContactDetails] = useState(event?.contactDetails ?? '');
  const [bannerFile, setBannerFile] = useState<File | null>(null);

  const [titleError, setTitleError] = useState<string | null>(null);
  const [eventDateError, setEventDateError] = useState<string | null>(null);
  const [timeError, setTimeError] = useState<string | null>(null);

  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const validate = (): boolean => {
    let valid = true;
    if (title.trim() === '') {
      setTitleError(t('events.titleRequired'));
      valid = false;
    } else {
      setTitleError(null);
    }
    if (eventDate === '') {
      setEventDateError(t('events.eventDateRequired'));
      valid = false;
    } else if (eventDate < todayIso()) {
      setEventDateError(t('events.eventDateInPast'));
      valid = false;
    } else {
      setEventDateError(null);
    }
    if (startTime === '' || endTime === '') {
      setTimeError(t('events.timeRequired'));
      valid = false;
    } else if (startTime >= endTime) {
      setTimeError(t('events.startBeforeEnd'));
      valid = false;
    } else {
      setTimeError(null);
    }
    return valid;
  };

  const handleSave = async () => {
    if (!validate()) return;

    setIsSaving(true);
    setError(null);
    try {
      const request = {
        title,
        description,
        category,
        eventDate,
        startTime: `${startTime}:00`,
        endTime: `${endTime}:00`,
        location,
        organizerName,
        contactDetails,
      };
      const saved = isEdit && event ? await eventsApi.update(event.id, request) : await eventsApi.create(request);
      if (bannerFile) {
        await eventsApi.uploadBanner(saved.id, bannerFile);
      }
      onSaved();
    } catch {
      setError(isEdit ? t('events.updateEventError') : t('events.createEventError'));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? t('events.editEvent') : t('events.createEvent')}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2.5} sx={{ mt: 0.5 }}>
          {error && <Alert severity="error">{error}</Alert>}

          <TextField
            label={t('events.eventTitle')}
            value={title}
            onChange={(e) => {
              setTitle(e.target.value);
              if (titleError) setTitleError(null);
            }}
            size="small"
            required
            error={Boolean(titleError)}
            helperText={titleError ?? undefined}
          />

          <TextField
            label={t('events.description')}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            size="small"
            multiline
            minRows={2}
          />

          <TextField
            label={t('events.category')}
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            size="small"
          />

          <TextField
            label={t('events.eventDate')}
            type="date"
            value={eventDate}
            onChange={(e) => {
              setEventDate(e.target.value);
              if (eventDateError) setEventDateError(null);
            }}
            size="small"
            InputLabelProps={{ shrink: true }}
            required
            error={Boolean(eventDateError)}
            helperText={eventDateError ?? undefined}
          />

          <Stack direction="row" spacing={2}>
            <TextField
              label={t('events.startTime')}
              type="time"
              value={startTime}
              onChange={(e) => {
                setStartTime(e.target.value);
                if (timeError) setTimeError(null);
              }}
              size="small"
              fullWidth
              InputLabelProps={{ shrink: true }}
              required
              error={Boolean(timeError)}
            />
            <TextField
              label={t('events.endTime')}
              type="time"
              value={endTime}
              onChange={(e) => {
                setEndTime(e.target.value);
                if (timeError) setTimeError(null);
              }}
              size="small"
              fullWidth
              InputLabelProps={{ shrink: true }}
              required
              error={Boolean(timeError)}
            />
          </Stack>
          {timeError && (
            <Typography variant="caption" color="error" sx={{ mt: -1.5 }}>
              {timeError}
            </Typography>
          )}

          <TextField
            label={t('events.location')}
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            size="small"
          />

          <TextField
            label={t('events.organizerName')}
            value={organizerName}
            onChange={(e) => setOrganizerName(e.target.value)}
            size="small"
          />

          <TextField
            label={t('events.contactDetails')}
            value={contactDetails}
            onChange={(e) => setContactDetails(e.target.value)}
            size="small"
          />

          <Stack spacing={0.5}>
            <Typography variant="body2" color="text.secondary">
              {t('events.banner')}
            </Typography>
            <input
              type="file"
              accept="image/jpeg,image/png"
              onChange={(e) => setBannerFile(e.target.files?.[0] ?? null)}
            />
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common.cancel')}</Button>
        <Button variant="contained" disabled={isSaving} onClick={() => void handleSave()}>
          {isSaving ? t('common.loading') : t('common.save')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

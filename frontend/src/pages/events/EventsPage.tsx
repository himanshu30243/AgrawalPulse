import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Button from '@mui/material/Button';
import AddIcon from '@mui/icons-material/Add';
import { useEventPermissions } from '@/auth/useEventPermissions';
import { BrowseEventsTab } from './BrowseEventsTab';
import { ManageEventsTab } from './ManageEventsTab';
import { EventFormDialog } from './EventFormDialog';

const TAB_BROWSE = 0;
const TAB_MANAGE = 1;

// One route-level page, admin tab gated internally via useEventPermissions - mirrors
// MembershipPage.tsx's shape. Unlike Membership's "My Status" (hidden for admins - a personal
// view an admin has no "own" version of), "Browse Events" is a shared community listing an admin
// would also want to see, so it is never hidden - only "Manage Events" is admin-only.
export default function EventsPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState(TAB_BROWSE);
  const [isCreating, setIsCreating] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const { canManageEvents } = useEventPermissions();

  const refresh = () => setRefreshKey((key) => key + 1);

  return (
    <Stack spacing={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={2}>
        <Stack spacing={0.5}>
          <Typography variant="h5" fontWeight={600}>
            {t('events.title')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t('events.subtitle')}
          </Typography>
        </Stack>
        {canManageEvents && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setIsCreating(true)}>
            {t('events.createEvent')}
          </Button>
        )}
      </Stack>

      <Tabs value={tab} onChange={(_, value: number) => setTab(value)}>
        <Tab label={t('events.browseTab')} value={TAB_BROWSE} />
        {canManageEvents && <Tab label={t('events.manageTab')} value={TAB_MANAGE} />}
      </Tabs>

      {tab === TAB_BROWSE && <BrowseEventsTab key={`browse-${refreshKey}`} />}
      {tab === TAB_MANAGE && canManageEvents && <ManageEventsTab key={`manage-${refreshKey}`} />}

      {isCreating && (
        <EventFormDialog
          onClose={() => setIsCreating(false)}
          onSaved={() => {
            setIsCreating(false);
            refresh();
          }}
        />
      )}
    </Stack>
  );
}

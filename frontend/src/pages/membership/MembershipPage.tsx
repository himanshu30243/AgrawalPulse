import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Button from '@mui/material/Button';
import AddIcon from '@mui/icons-material/Add';
import { useMembershipPermissions } from '@/auth/useMembershipPermissions';
import { MembershipStatusTab } from './MembershipStatusTab';
import { MembersListTab } from './MembersListTab';
import { PendingPaymentReportTab } from './PendingPaymentReportTab';
import { CollectionSummaryTab } from './CollectionSummaryTab';
import { RecordTransactionDialog } from './RecordTransactionDialog';

const TAB_STATUS = 0;
const TAB_MEMBERS = 1;
const TAB_PENDING_REPORT = 2;
const TAB_COLLECTION_SUMMARY = 3;

// One route-level page, admin tabs gated internally via useMembershipPermissions - mirrors
// FamiliesListPage's shape (the route admits every VIEW_MEMBERSHIP holder, the page itself
// shows/hides admin-only tabs and actions), rather than separate routes per tab.
export default function MembershipPage() {
  const { t } = useTranslation();
  const { canManageMembership, canViewMembershipAdminTools } = useMembershipPermissions();
  // "My Status" is a member-facing tab (requirement #1: view your own status/history) - an admin
  // never has "their own" membership in this context, so it's hidden for them entirely rather
  // than just being one tab among others. The route (AppRoutes.tsx) blocks this page behind
  // ProtectedRoute(permission=viewMembership) until GET /me has resolved, so this initial value is
  // normally already correct - but don't rely on that alone (nothing stops this component from
  // being reached before permissions resolve, e.g. rendered outside that route): the effect below
  // corrects a caller stuck on the now-hidden "My Status" tab once canViewMembershipAdminTools
  // settles, which also avoids MUI's "Tabs value doesn't match any child" warning.
  const [tab, setTab] = useState(canViewMembershipAdminTools ? TAB_MEMBERS : TAB_STATUS);
  const [isRecording, setIsRecording] = useState(false);

  useEffect(() => {
    if (canViewMembershipAdminTools && tab === TAB_STATUS) {
      setTab(TAB_MEMBERS);
    }
  }, [canViewMembershipAdminTools, tab]);

  return (
    <Stack spacing={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={2}>
        <Stack spacing={0.5}>
          <Typography variant="h5" fontWeight={600}>
            {t('membership.title')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t('membership.subtitle')}
          </Typography>
        </Stack>
        {canManageMembership && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setIsRecording(true)}>
            {t('membership.recordTransaction')}
          </Button>
        )}
      </Stack>

      <Tabs value={tab} onChange={(_, value: number) => setTab(value)}>
        {!canViewMembershipAdminTools && <Tab label={t('membership.statusTab')} value={TAB_STATUS} />}
        {canViewMembershipAdminTools && <Tab label={t('membership.membersTab')} value={TAB_MEMBERS} />}
        {canViewMembershipAdminTools && (
          <Tab label={t('membership.pendingReportTab')} value={TAB_PENDING_REPORT} />
        )}
        {canViewMembershipAdminTools && (
          <Tab label={t('membership.collectionSummaryTab')} value={TAB_COLLECTION_SUMMARY} />
        )}
      </Tabs>

      {tab === TAB_STATUS && !canViewMembershipAdminTools && <MembershipStatusTab />}
      {tab === TAB_MEMBERS && canViewMembershipAdminTools && <MembersListTab />}
      {tab === TAB_PENDING_REPORT && canViewMembershipAdminTools && <PendingPaymentReportTab />}
      {tab === TAB_COLLECTION_SUMMARY && canViewMembershipAdminTools && <CollectionSummaryTab />}

      {isRecording && (
        <RecordTransactionDialog onClose={() => setIsRecording(false)} onSaved={() => setIsRecording(false)} />
      )}
    </Stack>
  );
}

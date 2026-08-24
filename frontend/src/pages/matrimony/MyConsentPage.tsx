import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Paper from '@mui/material/Paper';
import Alert from '@mui/material/Alert';
import Switch from '@mui/material/Switch';
import FormControlLabel from '@mui/material/FormControlLabel';
import RadioGroup from '@mui/material/RadioGroup';
import Radio from '@mui/material/Radio';
import FormLabel from '@mui/material/FormLabel';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import Chip from '@mui/material/Chip';
import Snackbar from '@mui/material/Snackbar';
import InfoIcon from '@mui/icons-material/Info';
import { matrimonyApi } from '@/api/matrimonyApi';
import { useAsync } from '@/hooks/useAsync';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import type { ConsentScope } from '@/types/domain';

/**
 * Reachable by any authenticated MEMBER (see docs/api-specifications.md —
 * POST /matrimony/consent and DELETE /matrimony/consent/{familyMemberId} are
 * "self or guardian", not MATRIMONY_VIEWER-gated). This is deliberately a
 * separate route/nav entry from the Directory page: the DPDP consent flow
 * has to be reachable by the person consenting, not just by the viewer role
 * that later reads consented profiles.
 */
export default function MyConsentPage() {
  const { t } = useTranslation();
  const { data, isLoading, error, reload } = useAsync(() => matrimonyApi.getMyConsent(), []);

  const [pendingScope, setPendingScope] = useState<ConsentScope>('CHAPTER');
  const [isSaving, setIsSaving] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [toastSeverity, setToastSeverity] = useState<'success' | 'error'>('success');

  const grantConsent = async () => {
    setIsSaving(true);
    try {
      await matrimonyApi.setConsent(true, pendingScope);
      setToastSeverity('success');
      setToast(t('matrimony.consentSaved'));
      reload();
    } catch {
      setToastSeverity('error');
      setToast(t('matrimony.consentError'));
    } finally {
      setIsSaving(false);
    }
  };

  const withdrawConsent = async () => {
    setIsSaving(true);
    try {
      await matrimonyApi.setConsent(false, null);
      setToastSeverity('success');
      setToast(t('matrimony.consentSaved'));
      reload();
    } catch {
      setToastSeverity('error');
      setToast(t('matrimony.consentError'));
    } finally {
      setIsSaving(false);
    }
  };

  const requestErasure = async () => {
    try {
      await matrimonyApi.requestErasure();
      setToastSeverity('success');
      setToast(t('matrimony.consentSaved'));
    } catch {
      setToastSeverity('error');
      setToast(t('matrimony.consentError'));
    }
  };

  return (
    <Stack spacing={3}>
      <Stack spacing={0.5}>
        <Typography variant="h5" fontWeight={600}>
          {t('matrimony.consentPageTitle')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t('matrimony.consentPageSubtitle')}
        </Typography>
      </Stack>

      <LoadingErrorState isLoading={isLoading} error={error} onRetry={reload} />

      {data && (
        <>
          <Alert severity="info" icon={<InfoIcon />}>
            <Typography variant="subtitle2" fontWeight={600}>
              {t('matrimony.consentRequiredTitle')}
            </Typography>
            <Typography variant="body2">{t('matrimony.consentRequiredBody')}</Typography>
          </Alert>

          <Paper variant="outlined" sx={{ p: 3 }}>
            {!data.consentGiven ? (
              <Stack spacing={2}>
                <FormControlLabel
                  control={<Switch checked={false} disabled />}
                  label={t('matrimony.consentToggleLabel')}
                />
                <FormLabel id="consent-scope-label">{t('matrimony.consentScope')}</FormLabel>
                <RadioGroup
                  aria-labelledby="consent-scope-label"
                  value={pendingScope}
                  onChange={(e) => setPendingScope(e.target.value as ConsentScope)}
                >
                  <FormControlLabel
                    value="CHAPTER"
                    control={<Radio />}
                    label={t('matrimony.consentScopeChapter')}
                  />
                  <FormControlLabel
                    value="NATIONAL"
                    control={<Radio />}
                    label={t('matrimony.consentScopeNational')}
                  />
                </RadioGroup>
                <Button variant="contained" onClick={grantConsent} disabled={isSaving} sx={{ alignSelf: 'flex-start' }}>
                  {t('matrimony.consentGrant')}
                </Button>
              </Stack>
            ) : (
              <Stack spacing={2}>
                <FormControlLabel
                  control={<Switch checked disabled />}
                  label={t('matrimony.consentToggleLabel')}
                />
                <Stack direction="row" spacing={4} flexWrap="wrap">
                  <Stack>
                    <Typography variant="body2" color="text.secondary">
                      {t('matrimony.consentScope')}
                    </Typography>
                    <Chip
                      size="small"
                      label={
                        data.consentScope === 'NATIONAL'
                          ? t('matrimony.consentScopeNational')
                          : t('matrimony.consentScopeChapter')
                      }
                    />
                  </Stack>
                  <Stack>
                    <Typography variant="body2" color="text.secondary">
                      {t('matrimony.consentGivenBy')}
                    </Typography>
                    <Typography variant="body2">{data.consentGivenBy}</Typography>
                  </Stack>
                  <Stack>
                    <Typography variant="body2" color="text.secondary">
                      {t('matrimony.consentGivenAt')}
                    </Typography>
                    <Typography variant="body2">
                      {data.consentGivenAt ? new Date(data.consentGivenAt).toLocaleString('en-IN') : '—'}
                    </Typography>
                  </Stack>
                </Stack>
                <Divider />
                <Stack direction="row" spacing={2} flexWrap="wrap">
                  <Button color="warning" variant="outlined" onClick={withdrawConsent} disabled={isSaving}>
                    {t('matrimony.consentWithdraw')}
                  </Button>
                  <Button color="error" variant="text" onClick={requestErasure}>
                    {t('matrimony.requestErasure')}
                  </Button>
                </Stack>
                <Typography variant="caption" color="text.secondary">
                  {t('matrimony.erasureNote')}
                </Typography>
              </Stack>
            )}
          </Paper>
        </>
      )}

      <Snackbar open={Boolean(toast)} autoHideDuration={4000} onClose={() => setToast(null)}>
        <Alert severity={toastSeverity} onClose={() => setToast(null)}>
          {toast}
        </Alert>
      </Snackbar>
    </Stack>
  );
}

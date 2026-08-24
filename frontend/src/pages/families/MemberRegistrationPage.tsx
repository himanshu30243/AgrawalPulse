import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Grid from '@mui/material/Grid2';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import Divider from '@mui/material/Divider';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import AddIcon from '@mui/icons-material/Add';
import { familiesApi } from '@/api/familiesApi';
import { enumLabelKey } from './registration/enumLabelKey';
import type {
  BloodGroup,
  CreateFamilyMemberRequest,
  FamilyMember,
  Gender,
  MaritalStatus,
  RelationshipToHead,
} from '@/types/domain';

interface MemberRegistrationLocationState {
  familyId?: string;
  familyHeadName?: string;
  gotra?: string;
  moolGaon?: string;
}

const GENDERS: Gender[] = ['MALE', 'FEMALE', 'OTHER'];
const RELATIONSHIPS: RelationshipToHead[] = [
  'SPOUSE',
  'SON',
  'DAUGHTER',
  'FATHER',
  'MOTHER',
  'BROTHER',
  'SISTER',
  'OTHER',
];
const MARITAL_STATUSES: MaritalStatus[] = ['SINGLE', 'MARRIED', 'WIDOWED', 'DIVORCED'];
const BLOOD_GROUPS: BloodGroup[] = [
  'A_POSITIVE',
  'A_NEGATIVE',
  'B_POSITIVE',
  'B_NEGATIVE',
  'AB_POSITIVE',
  'AB_NEGATIVE',
  'O_POSITIVE',
  'O_NEGATIVE',
];

function emptyMember(): CreateFamilyMemberRequest {
  return {
    name: '',
    relationshipToHead: 'SON',
    dateOfBirth: '',
    gender: 'MALE',
    maritalStatus: 'SINGLE',
    education: '',
    instituteName: '',
    profession: '',
    companyName: '',
    mobileNumber: '',
    email: '',
    bloodGroup: null,
  };
}

export default function MemberRegistrationPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const familyId = searchParams.get('familyId') ?? '';

  const stateContext = (location.state as MemberRegistrationLocationState | null) ?? null;
  const [familyHeadName, setFamilyHeadName] = useState(stateContext?.familyHeadName ?? '');
  const [gotra, setGotra] = useState(stateContext?.gotra ?? '');
  const [moolGaon, setMoolGaon] = useState(stateContext?.moolGaon ?? '');

  const [members, setMembers] = useState<FamilyMember[]>([]);
  const [newMember, setNewMember] = useState<CreateFamilyMemberRequest>(emptyMember());
  const [errors, setErrors] = useState<Partial<Record<keyof CreateFamilyMemberRequest, string>>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState(false);

  const loadMembers = useCallback(() => {
    if (!familyId) return;
    familiesApi
      .listMembers(familyId)
      .then(setMembers)
      .catch(() => setMembers([]));
  }, [familyId]);

  useEffect(() => {
    loadMembers();
  }, [loadMembers]);

  useEffect(() => {
    // No router state (e.g. the page was refreshed, or opened directly from a bookmarked/shared
    // URL) - fall back to fetching the family so the header context isn't just blank.
    if (stateContext || !familyId) return;
    familiesApi
      .get(familyId)
      .then((family) => {
        setFamilyHeadName(family.headOfFamilyName);
        setGotra(family.gotra);
        setMoolGaon(family.nativePlace);
      })
      .catch(() => setLoadError(true));
    // Only ever needs to run once per familyId - stateContext intentionally excluded so a later
    // re-render doesn't re-trigger this fallback fetch.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [familyId]);

  const setField = <K extends keyof CreateFamilyMemberRequest>(key: K, value: CreateFamilyMemberRequest[K]) => {
    setNewMember((prev) => ({ ...prev, [key]: value }));
  };

  const handleAddMember = async () => {
    const nextErrors: typeof errors = {};
    if (!newMember.name.trim()) nextErrors.name = t('common.required');
    if (!newMember.dateOfBirth) nextErrors.dateOfBirth = t('common.required');
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitError(null);
    setIsSubmitting(true);
    try {
      await familiesApi.addMember(familyId, newMember);
      setNewMember(emptyMember());
      loadMembers();
    } catch {
      setSubmitError(t('families.registerError'));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!familyId) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        {t('families.memberRegistrationMissingFamily')}
      </Alert>
    );
  }

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        {t('families.memberRegistrationTitle')}
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        {t('families.memberRegistrationSubtitle')}
      </Typography>

      {loadError && <Alert severity="warning" sx={{ mb: 2 }}>{t('families.familyLoadError')}</Alert>}

      {familyHeadName && (
        <Alert severity="info" variant="outlined" sx={{ mb: 3 }}>
          {t('families.memberRegistrationContext', { name: familyHeadName, gotra, moolGaon })}
        </Alert>
      )}

      <Paper elevation={1} sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
        <Stack spacing={2}>
          <Typography variant="subtitle1" fontWeight={600}>
            {t('families.addMember')}
          </Typography>
          {submitError && <Alert severity="error">{submitError}</Alert>}
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                label={t('families.memberName')}
                value={newMember.name}
                onChange={(e) => setField('name', e.target.value)}
                error={Boolean(errors.name)}
                helperText={errors.name}
                fullWidth
                required
              />
            </Grid>
            <Grid size={{ xs: 6, sm: 3 }}>
              <TextField
                select
                label={t('families.relationshipToHead')}
                value={newMember.relationshipToHead ?? ''}
                onChange={(e) => setField('relationshipToHead', e.target.value as RelationshipToHead)}
                fullWidth
              >
                {RELATIONSHIPS.map((relationship) => (
                  <MenuItem key={relationship} value={relationship}>
                    {t(enumLabelKey('relation', relationship))}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField
                select
                label={t('families.gender')}
                value={newMember.gender}
                onChange={(e) => setField('gender', e.target.value as Gender)}
                fullWidth
              >
                {GENDERS.map((gender) => (
                  <MenuItem key={gender} value={gender}>
                    {t(enumLabelKey('gender', gender))}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 6, sm: 3 }}>
              <TextField
                label={t('families.dob')}
                type="date"
                value={newMember.dateOfBirth}
                onChange={(e) => setField('dateOfBirth', e.target.value)}
                error={Boolean(errors.dateOfBirth)}
                helperText={errors.dateOfBirth}
                fullWidth
                required
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField
                select
                label={t('families.maritalStatus')}
                value={newMember.maritalStatus ?? 'SINGLE'}
                onChange={(e) => setField('maritalStatus', e.target.value as MaritalStatus)}
                fullWidth
              >
                {MARITAL_STATUSES.map((status) => (
                  <MenuItem key={status} value={status}>
                    {t(enumLabelKey('marital', status))}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label={t('families.education')}
                value={newMember.education}
                onChange={(e) => setField('education', e.target.value)}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label={t('families.instituteName')}
                value={newMember.instituteName}
                onChange={(e) => setField('instituteName', e.target.value)}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label={t('families.profession')}
                value={newMember.profession}
                onChange={(e) => setField('profession', e.target.value)}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label={t('families.companyName')}
                value={newMember.companyName}
                onChange={(e) => setField('companyName', e.target.value)}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                label={t('families.mobileNumber')}
                value={newMember.mobileNumber}
                onChange={(e) => setField('mobileNumber', e.target.value.replace(/\D/g, '').slice(0, 10))}
                fullWidth
                inputMode="numeric"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                label={t('families.email')}
                value={newMember.email}
                onChange={(e) => setField('email', e.target.value)}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                select
                label={t('families.bloodGroup')}
                value={newMember.bloodGroup ?? ''}
                onChange={(e) => setField('bloodGroup', (e.target.value || null) as BloodGroup | null)}
                fullWidth
              >
                <MenuItem value="">{t('common.notSpecified')}</MenuItem>
                {BLOOD_GROUPS.map((group) => (
                  <MenuItem key={group} value={group}>
                    {t(enumLabelKey('blood', group))}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          </Grid>
          <Stack direction="row" justifyContent="flex-end">
            <Button variant="contained" startIcon={<AddIcon />} onClick={handleAddMember} disabled={isSubmitting}>
              {t('families.addMember')}
            </Button>
          </Stack>
        </Stack>
      </Paper>

      <Paper elevation={1} sx={{ p: { xs: 2, sm: 3 } }}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          {t('families.members')}
        </Typography>
        <Divider sx={{ mb: 2 }} />
        {members.length === 0 ? (
          <Typography color="text.secondary">{t('common.noData')}</Typography>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('families.memberName')}</TableCell>
                <TableCell>{t('families.relationshipToHead')}</TableCell>
                <TableCell>{t('families.age')}</TableCell>
                <TableCell>{t('families.gender')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {members.map((member) => (
                <TableRow key={member.id}>
                  <TableCell>{member.name}</TableCell>
                  <TableCell>
                    {member.relationshipToHead ? t(enumLabelKey('relation', member.relationshipToHead)) : '—'}
                  </TableCell>
                  <TableCell>{member.age}</TableCell>
                  <TableCell>{t(enumLabelKey('gender', member.gender))}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <Stack direction="row" justifyContent="flex-end" sx={{ mt: 3 }}>
          <Button variant="contained" onClick={() => navigate('/families')}>
            {t('families.memberRegistrationFinish')}
          </Button>
        </Stack>
      </Paper>
    </Box>
  );
}

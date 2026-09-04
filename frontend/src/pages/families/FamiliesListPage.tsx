import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableBody from '@mui/material/TableBody';
import TableContainer from '@mui/material/TableContainer';
import TablePagination from '@mui/material/TablePagination';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import InputAdornment from '@mui/material/InputAdornment';
import Alert from '@mui/material/Alert';
import AddIcon from '@mui/icons-material/Add';
import GroupIcon from '@mui/icons-material/Group';
import EditIcon from '@mui/icons-material/Edit';
import SearchIcon from '@mui/icons-material/Search';
import { familiesApi } from '@/api/familiesApi';
import { useAsync } from '@/hooks/useAsync';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { useAuth } from '@/auth/useAuth';
import { useFamilyPermissions } from '@/auth/useFamilyPermissions';
import { enumLabelKey } from './registration/enumLabelKey';
import { EditFamilyDialog } from './EditFamilyDialog';
import type { Family, FamilyCategory } from '@/types/domain';

const ALL = 'ALL';

const FAMILY_CATEGORIES: FamilyCategory[] = [
  'BUSINESS',
  'SALARIED',
  'PROFESSIONAL',
  'RETIRED',
  'AGRICULTURE',
  'OTHER',
];

function BranchCell({ family }: { family: Family }) {
  const { t } = useTranslation();
  if (!family.branch) {
    // BranchClient degrades to null on lookup failure (see backend/family-service/BranchClient) -
    // never treat a missing branch as an error, just show the raw id as a fallback.
    return <Chip label={family.chapterId} size="small" variant="outlined" title={t('families.branchUnavailable')} />;
  }
  return <Chip label={`${family.branch.name} · ${family.branch.city}`} size="small" />;
}

function FamilyMembersDialog({ family, onClose }: { family: Family; onClose: () => void }) {
  const { t } = useTranslation();
  const { data, isLoading, error } = useAsync(() => familiesApi.listMembers(family.id), [family.id]);

  return (
    <Dialog open onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        {t('families.membersOf', { name: family.headOfFamilyName })}
        {family.familyCode ? ` (${family.familyCode})` : ''}
      </DialogTitle>
      <DialogContent dividers>
        <LoadingErrorState isLoading={isLoading} error={error} onRetry={() => undefined} />
        {data && data.length === 0 && (
          <Typography color="text.secondary">{t('common.noData')}</Typography>
        )}
        {data && data.length > 0 && (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('families.memberName')}</TableCell>
                  <TableCell>{t('families.relationshipToHead')}</TableCell>
                  <TableCell align="right">{t('families.age')}</TableCell>
                  <TableCell>{t('families.gender')}</TableCell>
                  <TableCell>{t('families.maritalStatus')}</TableCell>
                  <TableCell>{t('families.education')}</TableCell>
                  <TableCell>{t('families.profession')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.map((member) => (
                  <TableRow key={member.id}>
                    <TableCell>{member.name}</TableCell>
                    <TableCell>
                      {member.relationshipToHead ? t(enumLabelKey('relation', member.relationshipToHead)) : '—'}
                    </TableCell>
                    <TableCell align="right">{member.age}</TableCell>
                    <TableCell>{t(enumLabelKey('gender', member.gender))}</TableCell>
                    <TableCell>{t(enumLabelKey('marital', member.maritalStatus))}</TableCell>
                    <TableCell>{member.education || '—'}</TableCell>
                    <TableCell>{member.profession || '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DialogContent>
    </Dialog>
  );
}

export default function FamiliesListPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data, isLoading, error, reload } = useAsync(() => familiesApi.list(), []);
  const [selectedFamily, setSelectedFamily] = useState<Family | null>(null);
  const [editingFamily, setEditingFamily] = useState<Family | null>(null);

  const [search, setSearch] = useState('');
  const [category, setCategory] = useState<FamilyCategory | typeof ALL>(ALL);
  const [branchId, setBranchId] = useState<string>(ALL);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  // Stable identity so useFamilyPermissions' memo isn't invalidated every render.
  const families = useMemo(() => data ?? [], [data]);
  const { isProfileLoaded } = useAuth();
  const { visibleFamilies, canCreateFamily, createDenial, canEditFamily } = useFamilyPermissions(families);

  // Only offer branches that actually appear in what this user can see - a member scoped to one
  // family would otherwise get a filter listing branches they can't reach.
  const branchOptions = useMemo(() => {
    const seen = new Map<string, string>();
    for (const family of visibleFamilies) {
      if (!seen.has(family.chapterId)) {
        seen.set(family.chapterId, family.branch ? `${family.branch.name} · ${family.branch.city}` : family.chapterId);
      }
    }
    return [...seen.entries()].map(([id, label]) => ({ id, label }));
  }, [visibleFamilies]);

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    return visibleFamilies.filter((family) => {
      if (category !== ALL && family.familyCategory !== category) return false;
      if (branchId !== ALL && family.chapterId !== branchId) return false;
      if (!query) return true;
      return [family.headOfFamilyName, family.familyCode, family.mobileNumber, family.city, family.email]
        .some((field) => field?.toLowerCase().includes(query));
    });
  }, [visibleFamilies, search, category, branchId]);

  const filtersActive = search.trim() !== '' || category !== ALL || branchId !== ALL;

  const clearFilters = () => {
    setSearch('');
    setCategory(ALL);
    setBranchId(ALL);
    setPage(0);
  };

  // Client-side pagination over the already-filtered list - the backend has no pagination
  // support anywhere in this codebase yet (every list endpoint returns its full scoped result),
  // so this is purely a render-cost/usability improvement, not a network optimization.
  const paginated = useMemo(
    () => filtered.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage),
    [filtered, page, rowsPerPage],
  );

  // Defensive clamp: if the result set shrinks (a reload returns fewer rows, or rowsPerPage
  // changes) while sitting on a now out-of-range page, land back on the last valid page instead
  // of rendering blank.
  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(filtered.length / rowsPerPage) - 1);
    if (page > maxPage) setPage(maxPage);
  }, [filtered.length, rowsPerPage, page]);

  // Any filter change can shrink the result set out from under the current page (e.g. page 3 of
  // an unfiltered list may not exist once a search narrows it to one page) - reset to the first
  // page whenever the filters themselves change, not on every `filtered` identity change (which
  // would also fire on a background reload of the same data).
  const handleSearchChange = (value: string) => {
    setSearch(value);
    setPage(0);
  };
  const handleCategoryChange = (value: FamilyCategory | typeof ALL) => {
    setCategory(value);
    setPage(0);
  };
  const handleBranchChange = (value: string) => {
    setBranchId(value);
    setPage(0);
  };

  return (
    <Stack spacing={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={2}>
        <Stack spacing={0.5}>
          <Typography variant="h5" fontWeight={600}>
            {t('families.title')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t('families.subtitle')}
          </Typography>
        </Stack>
        {/*
          Hidden outright for roles with no create right at all (treasurer, matrimony viewer);
          shown-but-disabled when the role may create but has hit the per-user cap, so the reason
          is discoverable rather than the button just vanishing.
        */}
        {isProfileLoaded && createDenial !== 'no-permission' && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            disabled={!canCreateFamily}
            onClick={() => navigate('/families/register')}
          >
            {t('families.registerNew')}
          </Button>
        )}
      </Stack>

      {isProfileLoaded && createDenial === 'limit-reached' && (
        <Alert severity="info">{t('families.registrationLimitReached')}</Alert>
      )}

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
        <TextField
          label={t('families.search')}
          placeholder={t('families.searchPlaceholder')}
          value={search}
          onChange={(e) => handleSearchChange(e.target.value)}
          size="small"
          fullWidth
          sx={{ maxWidth: { md: 360 } }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" />
              </InputAdornment>
            ),
          }}
        />
        <TextField
          select
          label={t('families.familyCategory')}
          value={category}
          onChange={(e) => handleCategoryChange(e.target.value as FamilyCategory | typeof ALL)}
          size="small"
          fullWidth
          sx={{ maxWidth: { md: 220 } }}
        >
          <MenuItem value={ALL}>{t('families.allCategories')}</MenuItem>
          {FAMILY_CATEGORIES.map((value) => (
            <MenuItem key={value} value={value}>
              {t(enumLabelKey('category', value))}
            </MenuItem>
          ))}
        </TextField>
        {branchOptions.length > 1 && (
          <TextField
            select
            label={t('families.chapter')}
            value={branchId}
            onChange={(e) => handleBranchChange(e.target.value)}
            size="small"
            fullWidth
            sx={{ maxWidth: { md: 260 } }}
          >
            <MenuItem value={ALL}>{t('families.allBranches')}</MenuItem>
            {branchOptions.map((option) => (
              <MenuItem key={option.id} value={option.id}>
                {option.label}
              </MenuItem>
            ))}
          </TextField>
        )}
        {filtersActive && (
          <Button onClick={clearFilters} size="small">
            {t('families.clearFilters')}
          </Button>
        )}
      </Stack>

      <LoadingErrorState isLoading={isLoading} error={error} onRetry={reload} />

      {data && filtered.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">
            {filtersActive ? t('families.noMatches') : t('common.noData')}
          </Typography>
        </Paper>
      )}

      {data && filtered.length > 0 && (
        <>
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('families.sequenceNo')}</TableCell>
                  <TableCell>{t('families.familyCode')}</TableCell>
                  <TableCell>{t('families.headOfFamily')}</TableCell>
                  <TableCell>{t('common.chapter')}</TableCell>
                  <TableCell>{t('families.familyCategory')}</TableCell>
                  <TableCell>{t('families.city')}</TableCell>
                  <TableCell>{t('common.date')}</TableCell>
                  <TableCell align="right">{t('common.actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {paginated.map((family, index) => (
                  <TableRow key={family.id} hover>
                    <TableCell>{page * rowsPerPage + index + 1}</TableCell>
                    <TableCell>{family.familyCode || family.id}</TableCell>
                    <TableCell>{family.headOfFamilyName}</TableCell>
                    <TableCell>
                      <BranchCell family={family} />
                    </TableCell>
                    <TableCell>
                      {family.familyCategory ? t(enumLabelKey('category', family.familyCategory)) : '—'}
                    </TableCell>
                    <TableCell>{family.city || '—'}</TableCell>
                    <TableCell>{family.createdAt ? new Date(family.createdAt).toLocaleDateString('en-IN') : '—'}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        {canEditFamily(family) && (
                          <Button
                            size="small"
                            startIcon={<EditIcon />}
                            onClick={() => setEditingFamily(family)}
                          >
                            {t('common.edit')}
                          </Button>
                        )}
                        <Button
                          size="small"
                          startIcon={<GroupIcon />}
                          onClick={() => setSelectedFamily(family)}
                        >
                          {t('families.viewMembers')}
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={filtered.length}
            page={page}
            onPageChange={(_, newPage) => setPage(newPage)}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={(e) => {
              setRowsPerPage(Number(e.target.value));
              setPage(0);
            }}
            rowsPerPageOptions={[10, 25, 50, 100]}
            labelRowsPerPage={t('common.rowsPerPage')}
            labelDisplayedRows={({ from, to, count }) => t('common.paginationDisplayedRows', { from, to, count })}
          />
        </>
      )}

      {selectedFamily && (
        <FamilyMembersDialog family={selectedFamily} onClose={() => setSelectedFamily(null)} />
      )}

      {editingFamily && (
        <EditFamilyDialog
          family={editingFamily}
          onClose={() => setEditingFamily(null)}
          onSaved={() => {
            setEditingFamily(null);
            reload();
          }}
        />
      )}
    </Stack>
  );
}

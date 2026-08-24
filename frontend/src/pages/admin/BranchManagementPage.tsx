import { useCallback, useMemo, useState } from 'react';
import Add from '@mui/icons-material/Add';
import Edit from '@mui/icons-material/Edit';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';
import IconButton from '@mui/material/IconButton';
import Alert from '@mui/material/Alert';
import { useAsync } from '@/hooks/useAsync';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { chaptersApi } from '@/api/rbacApi';
import type { Chapter } from '@/api/rbacApi';

const EMPTY_FORM = { name: '', city: '', state: '' };

export default function BranchManagementPage() {
  const { data: branches, isLoading, error, reload } = useAsync(() => chaptersApi.list(), []);

  const [mode, setMode] = useState<'create' | 'edit' | null>(null);
  const [editing, setEditing] = useState<Chapter | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [search, setSearch] = useState('');

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return branches ?? [];
    return (branches ?? []).filter(
      (branch) =>
        branch.name.toLowerCase().includes(query) ||
        branch.city.toLowerCase().includes(query) ||
        branch.state.toLowerCase().includes(query),
    );
  }, [branches, search]);

  const close = () => {
    setMode(null);
    setEditing(null);
    setFormError(null);
  };

  const openCreate = () => {
    setForm(EMPTY_FORM);
    setEditing(null);
    setFormError(null);
    setMode('create');
  };

  const openEdit = (branch: Chapter) => {
    setForm({ name: branch.name, city: branch.city, state: branch.state });
    setEditing(branch);
    setFormError(null);
    setMode('edit');
  };

  const save = useCallback(async () => {
    if (!form.name.trim() || !form.city.trim() || !form.state.trim()) {
      setFormError('Name, city and state are all required.');
      return;
    }
    setBusy(true);
    setFormError(null);
    const payload = { name: form.name.trim(), city: form.city.trim(), state: form.state.trim() };
    try {
      if (mode === 'edit' && editing) {
        await chaptersApi.update(editing.id, payload);
      } else {
        await chaptersApi.create(payload);
      }
      reload();
      close();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'The branch could not be saved.');
    } finally {
      setBusy(false);
    }
  }, [form, mode, editing, reload]);

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={2} sx={{ mb: 3 }}>
        <Stack spacing={0.5}>
          <Typography variant="h5" fontWeight={600}>
            Branch Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Branches (chapters) that families and users belong to
          </Typography>
        </Stack>
        <Button variant="contained" startIcon={<Add />} onClick={openCreate}>
          Create Branch
        </Button>
      </Stack>

      {/*
        No delete action anywhere on this screen: chapter_id is the tenant boundary that families,
        memberships, events and matrimony all key off, with no FK back to this table. Removing a
        branch would orphan those rows silently, so the backend deliberately exposes no delete
        endpoint (see ChapterController).
      */}
      <Alert severity="info" sx={{ mb: 3 }}>
        Branches cannot be deleted — families, memberships and events reference them. Contact an
        administrator if a branch was created in error.
      </Alert>

      <TextField
        fullWidth
        size="small"
        placeholder="Search by name, city or state"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        sx={{ mb: 3, maxWidth: 420 }}
      />

      <LoadingErrorState isLoading={isLoading} error={error} onRetry={reload} />

      {branches && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>City</TableCell>
                <TableCell>State</TableCell>
                <TableCell>Created</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((branch) => (
                <TableRow key={branch.id} hover>
                  <TableCell sx={{ fontWeight: 500 }}>{branch.name}</TableCell>
                  <TableCell>{branch.city}</TableCell>
                  <TableCell>{branch.state}</TableCell>
                  <TableCell>
                    {branch.createdAt ? new Date(branch.createdAt).toLocaleDateString('en-IN') : '—'}
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Edit branch">
                      <IconButton size="small" onClick={() => openEdit(branch)} aria-label={`Edit ${branch.name}`}>
                        <Edit fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {branches && filtered.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">No branches match your search.</Typography>
        </Paper>
      )}

      <Dialog open={mode !== null} onClose={close} maxWidth="xs" fullWidth>
        <DialogTitle>{mode === 'edit' ? 'Edit Branch' : 'Create Branch'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {formError && <Alert severity="error">{formError}</Alert>}
            <TextField
              label="Name"
              value={form.name}
              onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
              placeholder="e.g. Indore Chapter"
              fullWidth
            />
            <TextField
              label="City"
              value={form.city}
              onChange={(e) => setForm((prev) => ({ ...prev, city: e.target.value }))}
              fullWidth
            />
            <TextField
              label="State"
              value={form.state}
              onChange={(e) => setForm((prev) => ({ ...prev, state: e.target.value }))}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" onClick={() => void save()} disabled={busy}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
